package eventer.application

import cats.MonoidK.ops.toAllMonoidKOps
import cats.data.{Kleisli, OptionT}
import cats.effect.ExitCode
import eventer.application.WebServer._
import eventer.domain._
import io.circe.syntax.EncoderOps
import javax.crypto.{KeyGenerator, SecretKey}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CSRF.{CSRFBuilder, SigningAlgo}
import org.http4s.server.middleware.CSRF
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

class WebServer[R](eventRepository: EventRepository[R],
                   sessionService: SessionService[R],
                   generateEventId: UIO[EventId],
                   csrfKey: SecretKey) {
  type IO[A] = RIO[R with Clock, A]
  type OptionTIO = { type T[A] = OptionT[IO, A] }

  private[application] val csrf: CSRF[OptionTIO#T, IO] = {
    val key = KeyGenerator.getInstance(SigningAlgo).generateKey()
    // Double submit cookie is enough, no need to check Origin header on top of that.
    val csrfBuilder: CSRFBuilder[OptionTIO#T, IO] = CSRF(key, _ => true)
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
      jwtHeaderPayload <- UIO(request.cookies.find(_.name == JwtHeaderPayloadCookieName).map(_.content)).someOrFailException
      jwtSignature <- UIO(request.cookies.find(_.name == JwtSignatureCookieName).map(_.content)).someOrFailException
      (jwtHeader, jwtPayload) <- UIO(Some(jwtHeaderPayload).map(_.split('.')).collect { case Array(x, y) => (x, y) }).someOrFailException
      contentJson <- sessionService
        .decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
        .someOrFailException
      sessionUser <- UIO(io.circe.parser.decode[SessionUser](contentJson).toOption).someOrFailException
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
            clock <- ZIO.environment[Clock]
            now <- clock.clock.currentDateTime
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
          sessionUserOption <- sessionService.login(loginRequest)
          response <- sessionUserOption.fold(Forbidden())(successResponse)
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

    val csrfMiddleware: Middleware[OptionTIO#T, Request[IO], Response[IO], Request[IO], Response[IO]] = csrf.validate()

    csrfMiddleware(publicRoutes <+> authMiddleware(authedRoutes)).orNotFound
  }

  def serve(port: Int): RIO[R with Clock, Unit] = {
    import zio.interop.catz._
    ZIO.runtime[R with Clock].flatMap { implicit clock =>
      BlazeServerBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(routes)
        .serve
        .compile[IO, IO, ExitCode]
        .drain
    }
  }
}
