package eventer.application

import cats.MonoidK.ops.toAllMonoidKOps
import cats.data.{Kleisli, OptionT}
import cats.effect.ExitCode
import eventer.domain._
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import zio.clock.Clock
import zio.interop.catz._
import zio.{RIO, UIO, URIO, ZIO}

class WebServer[R](eventRepository: EventRepository[R],
                   sessionService: SessionService[R],
                   csrfTokenGenerator: URIO[R, String]) {
  type IO[A] = RIO[R with Clock, A]
  private val JwtSignatureCookieName = "jwt-signature"
  private val JwtHeaderPayloadCookieName = "jwt-header.payload"
  private val CsrfTokenCookieName = "csrf-token"

  private[application] val routes: HttpApp[IO] = {

    val codecs = new Codecs[IO]
    import codecs._

    val dsl = Http4sDsl[IO]
    import dsl._
    val publicRoutes = HttpRoutes.of[IO] {
      case GET -> Root / "events" =>
        eventRepository.findAll.flatMap(events => Ok(events))

      case request @ POST -> Root / "sessions" =>
        def successResponse(loginResponse: LoginResponse): RIO[R with Clock, Response[IO]] =
          for {
            clock <- ZIO.environment[Clock]
            now <- clock.clock.currentDateTime
            expiresAt = now.plusDays(30)
            jwtHeaderPayloadSignature <- sessionService.encodedJwtHeaderPayloadSignature(loginResponse.asJson.noSpaces,
                                                                                         now.toInstant,
                                                                                         expiresAt.toInstant)
            (header, payload, signature) = jwtHeaderPayloadSignature // for some reason we cannot pattern match above
            csrfToken <- csrfTokenGenerator
            response <- Created(loginResponse)
          } yield {
            def makeCookie(name: String, content: String, httpOnly: Boolean) =
              ResponseCookie(name, content, maxAge = Some(expiresAt.toEpochSecond), secure = true, httpOnly = httpOnly)
            response
              .addCookie(makeCookie(JwtSignatureCookieName, signature, httpOnly = true))
              .addCookie(makeCookie(JwtHeaderPayloadCookieName, header + "." + payload, httpOnly = false))
              .addCookie(makeCookie(CsrfTokenCookieName, csrfToken, httpOnly = false))
          }

        for {
          loginRequest <- request.as[LoginRequest]
          loginResponseOption <- sessionService.login(loginRequest)
          response <- loginResponseOption.fold(Forbidden())(successResponse)
        } yield response
    }

    val authedRoutes = AuthedRoutes.of[User, IO] {
      case request @ POST -> Root / "events" as user =>
        for {
          event <- request.req.as[Event]
          _ <- eventRepository.create(event)
          response <- Created()
        } yield response
    }

    type User = String
    val authUser: Kleisli[({ type T[A] = OptionT[IO, A] })#T, Request[IO], User] = Kleisli { request =>
      val nameM = for {
        jwtHeaderPayload <- UIO(request.cookies.find(_.name == JwtHeaderPayloadCookieName).map(_.content)).someOrFailException
        jwtSignature <- UIO(request.cookies.find(_.name == JwtSignatureCookieName).map(_.content)).someOrFailException
        (jwtHeader, jwtPayload) <- UIO(Some(jwtHeaderPayload).map(_.split('.')).collect { case Array(x, y) => (x, y) }).someOrFailException
        now <- ZIO.accessM[Clock](_.clock.currentDateTime).map(_.toInstant)
        contentJson <- sessionService
          .decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature, now)
          .someOrFailException
        loginResponse <- UIO(io.circe.parser.decode[LoginResponse](contentJson).toOption).someOrFailException
      } yield loginResponse.name
      OptionT(nameM.option)
    }

    // Per default Http4s returns `Unauthorized` which per spec requires a `WWW-Authenticate` header but Http4s doesn't
    // supply it. That's against the spec and that's not cool. Also, that header doesn't make sense for our form based
    // authentication and so the `Unauthorized` HTTP code is inappropriate. We use `Forbidden` instead.
    val authMiddleware = AuthMiddleware.noSpider(authUser, (_: Request[IO]) => Forbidden())

    (publicRoutes <+> authMiddleware(authedRoutes)).orNotFound
  }

  def serve(port: Int): RIO[R with Clock, Unit] = {
    import zio.interop.catz._
    ZIO.runtime[R with Clock].flatMap { implicit clock =>
      BlazeServerBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(CORS(routes))
        .serve
        .compile[IO, IO, ExitCode]
        .drain
    }
  }
}
