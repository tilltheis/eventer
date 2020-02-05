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
import zio.{RIO, ZIO}

class WebServer[R](eventRepository: EventRepository[R], sessionService: SessionService[R]) {
  type IO[A] = RIO[R with Clock, A]

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
            expiresInSeconds = now.plusDays(30).toEpochSecond - now.toEpochSecond
            jwtHeaderPayloadSignature <- sessionService.encodedJwtHeaderPayloadSignature(loginResponse.asJson.noSpaces,
                                                                                         now.toEpochSecond,
                                                                                         expiresInSeconds)
            (header, payload, signature) = jwtHeaderPayloadSignature // for some reason we cannot pattern match above
            response <- Created(loginResponse)
          } yield {
            def makeCookie(name: String, content: String, httpOnly: Boolean) =
              ResponseCookie(name, content, maxAge = Some(expiresInSeconds), secure = true, httpOnly = httpOnly)
            response
              .addCookie(makeCookie("jwt-signature", signature, httpOnly = true))
              .addCookie(makeCookie("jwt-header.payload", header + "." + payload, httpOnly = false))
              .addCookie(makeCookie("csrf-token", "csrf-token", httpOnly = false))
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
    val authUser: Kleisli[({ type T[A] = OptionT[IO, A] })#T, Request[IO], User] =
      Kleisli(_ => OptionT.liftF(RIO.succeed("User")))
    val authMiddleware = AuthMiddleware(authUser)

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
