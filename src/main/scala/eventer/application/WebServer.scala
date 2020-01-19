package eventer.application

import cats.effect.ExitCode
import eventer.domain.{EventId, EventRepository}
import io.circe.Encoder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpApp, HttpRoutes}
import zio.clock.Clock
import zio.{RIO, ZIO}

class WebServer[R](eventRepository: EventRepository[R]) {
  type IO[A] = RIO[R with Clock, A]

  private val routes: HttpApp[IO] = {
    import io.circe.generic.auto._
    import io.circe.syntax._
    import org.http4s.circe._
    import org.http4s.dsl.Http4sDsl
    import zio.interop.catz._

    implicit val encodeEventId: Encoder[EventId] = (id: EventId) => id.id.toString.asJson

    val dsl = Http4sDsl[IO]
    import dsl._
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / "events" =>
        eventRepository.findEvents.flatMap(events => {
          Ok(events.asJson)
        })
    }

    routes.orNotFound
  }

  val serve: IO[Unit] = {
    import zio.interop.catz._

    ZIO.runtime[Clock with R].flatMap { implicit clock =>
      BlazeServerBuilder[IO]
        .bindHttp(8080)
        .withHttpApp(CORS(routes))
        .serve
        .compile[IO, IO, ExitCode]
        .drain
    }
  }
}
