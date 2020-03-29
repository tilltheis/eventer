package eventer.application

import cats.MonoidK.ops.toAllMonoidKOps
import cats.data.{Kleisli, OptionT}
import cats.effect.ExitCode
import com.typesafe.scalalogging.StrictLogging
import eventer.application.WebServer._
import eventer.domain._
import io.circe.syntax.EncoderOps
import javax.crypto.SecretKey
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CSRF
import org.http4s.server.middleware.CSRF.CSRFBuilder
import org.http4s.server.{AuthMiddleware, Middleware}
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import org.http4s.util.CaseInsensitiveString
import zio.clock.Clock
import zio.interop.catz._
import zio.{RIO, UIO, ZIO}

object WebServer {
  val CsrfSigningAlgorithm: String = CSRF.SigningAlgo

  private[application] val JwtSignatureCookieName = "jwt-signature"
  private[application] val JwtHeaderPayloadCookieName = "jwt-header.payload"
  private[application] val CsrfTokenCookieName = "csrf-token"
  private[application] val CsrfTokenHeaderName = "X-Csrf-Token"
}

class WebServer[R, HashT](eventRepository: EventRepository[R],
                          sessionService: SessionService[R],
                          userRepository: UserRepository[R, HashT],
                          cryptoHashing: CryptoHashing[HashT],
                          generateEventId: UIO[EventId],
                          generateUserId: UIO[UserId],
                          csrfKey: SecretKey)
    extends StrictLogging {
  type IO[A] = RIO[R with Clock, A]
  type OptionTIO = { type T[A] = OptionT[IO, A] }

  private[application] val csrf: CSRF[OptionTIO#T, IO] = {
    // Double submit cookie is enough, no need to check Origin header on top of that.
    val csrfBuilder: CSRFBuilder[OptionTIO#T, IO] = CSRF(csrfKey, _ => true)
    csrfBuilder
      .withCookieName(CsrfTokenCookieName)
      .withHeaderName(CaseInsensitiveString(CsrfTokenHeaderName))
      .withCookiePath(None)
      .withCookieHttpOnly(false)
      .withCookieSecure(true)
      .build
  }

  private val authUser: Kleisli[OptionTIO#T, Request[IO], SessionUser] = Kleisli { request =>
    val codecs = new Codecs[IO]
    import codecs._
    val sessionUserM = for {
      jwtHeaderPayload <- UIO(request.cookies.find(_.name == JwtHeaderPayloadCookieName).map(_.content)).get
      jwtSignature <- UIO(request.cookies.find(_.name == JwtSignatureCookieName).map(_.content)).get
      jwtHeaderAndPayload <- UIO(Some(jwtHeaderPayload).map(_.split('.')).collect { case Array(x, y) => (x, y) }).get
      (jwtHeader, jwtPayload) = jwtHeaderAndPayload
      contentJson <- sessionService.decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
      sessionUser <- UIO.succeed(io.circe.parser.decode[SessionUser](contentJson).toOption).get
    } yield sessionUser
    OptionT(sessionUserM.option)
  }

  private[application] val routes: HttpApp[IO] = {
    val codecs = new Codecs[IO]
    import codecs._

    val dsl = Http4sDsl[IO]
    import dsl._
    val publicRoutes = HttpRoutes.of[IO] {
      case GET -> Root / "events" =>
        eventRepository.findAll.flatMap(events => Ok(events))

      case request @ POST -> Root / "sessions" =>
        def successResponse(sessionUser: SessionUser): RIO[R with Clock, Response[IO]] =
          for {
            now <- ZIO.accessM[Clock](_.get.currentDateTime)
            expiresAt = now.plusDays(30)
            expiresInSeconds = expiresAt.toEpochSecond - now.toEpochSecond
            jwtHeaderPayloadSignature <- sessionService.encodedJwtHeaderPayloadSignature(sessionUser.asJson.noSpaces,
                                                                                         expiresAt.toInstant)
            (header, payload, signature) = jwtHeaderPayloadSignature // for some reason we cannot pattern match above
            response <- Created()
          } yield {
            def makeCookie(name: String, content: String, httpOnly: Boolean) =
              ResponseCookie(name, content, maxAge = Some(expiresInSeconds), secure = true, httpOnly = httpOnly)
            response
              .addCookie(makeCookie(JwtSignatureCookieName, signature, httpOnly = true))
              .addCookie(makeCookie(JwtHeaderPayloadCookieName, header + "." + payload, httpOnly = false))
          }

        for {
          loginRequest <- request.as[LoginRequest]
          sessionUserOption <- sessionService.login(loginRequest).option
          response <- sessionUserOption.fold(Forbidden())(successResponse)
        } yield response

      case request @ POST -> Root / "users" =>
        for {
          registrationRequest <- request.as[RegistrationRequest]
          id <- generateUserId
          passwordHash <- cryptoHashing.hash(registrationRequest.password)
          _ <- userRepository.create(registrationRequest.toUser(id, passwordHash)).option
          response <- Created() // always pretend to have created an account to not leak data
        } yield response
    }

    val authedRoutes = AuthedRoutes.of[SessionUser, IO] {
      case DELETE -> Root / "sessions" as _ =>
        def removeCookie(name: String, httpOnly: Boolean) =
          ResponseCookie(name, "", expires = Some(HttpDate.Epoch), maxAge = Some(0), secure = true, httpOnly = httpOnly)
        Ok().map(
          _.addCookie(removeCookie(JwtHeaderPayloadCookieName, httpOnly = false))
            .addCookie(removeCookie(JwtSignatureCookieName, httpOnly = true)))

      case request @ POST -> Root / "events" as sessionUser =>
        for {
          eventCreationRequest <- request.req.as[EventCreationRequest]
          id <- generateEventId
          event = eventCreationRequest.toEvent(id, sessionUser.id)
          _ <- eventRepository.create(event)
          response <- Created()
        } yield response
    }

    // Per default Http4s returns `Unauthorized` which per spec requires a `WWW-Authenticate` header but Http4s doesn't
    // supply it. That's against the spec and that's not cool. Also, that header doesn't make sense for our form based
    // authentication and so the `Unauthorized` HTTP code is inappropriate. We use `Forbidden` instead.
    val authMiddleware
      : Middleware[OptionTIO#T, AuthedRequest[IO, SessionUser], Response[IO], Request[IO], Response[IO]] =
      AuthMiddleware.noSpider(authUser, _ => Forbidden())

    // Having CSRF is more like an additional safety net because we're only planning to have an SPA and that is
    // practically safe against CSRF attacks. The CSRF attack vector only opens when we allow
    // `application/x-www-form-urlencoded` instead of `application/json`.
    // However, HTTP4S currently ignores the content type when decoding requests as JSON and I don't know how to change
    // that. Until either problem is solved we actually have to have the CSRF token in order to be safe.
    val csrfMiddleware: Middleware[OptionTIO#T, Request[IO], Response[IO], Request[IO], Response[IO]] = csrf.validate()

    csrfMiddleware(publicRoutes <+> authMiddleware(authedRoutes)).orNotFound
  }

  def serve(port: Int): RIO[R with Clock, Unit] = {
    import zio.interop.catz._
    ZIO.runtime[R with Clock].flatMap { implicit clock =>
      BlazeServerBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(routes.mapF(_.absorb))
        .withServiceErrorHandler(request => {
          case throwable =>
            logger.error(s"Error handling request ${request.method.toString} ${request.uri.toString}", throwable)
            UIO.succeed(Response(Status.InternalServerError))
        })
        .serve
        .compile[IO, IO, ExitCode]
        .drain
    }
  }
}
