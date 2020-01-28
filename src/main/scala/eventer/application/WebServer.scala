package eventer.application

import cats.effect.ExitCode
import eventer.domain.{Event, EventRepository}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpApp, HttpRoutes}
import zio.clock.Clock
import zio.{RIO, ZIO}

class WebServer[R](eventRepository: EventRepository[R]) {
  private[application] def routes[R0 <: R]: HttpApp[({ type T[A] = RIO[R0, A] })#T] = {
    import org.http4s.circe._
    import org.http4s.dsl.Http4sDsl
    import zio.interop.catz._

    val codecs = new Codecs[({ type T[A] = RIO[R0, A] })#T]
    import codecs._

    val dsl = Http4sDsl[({ type T[A] = RIO[R0, A] })#T]
    import dsl._
    val routes = HttpRoutes.of[({ type T[A] = RIO[R0, A] })#T] {
      case GET -> Root / "events" =>
        eventRepository.findAll.flatMap(events => { Ok(events) })

      case request @ POST -> Root / "events" =>
        for {
          event <- request.as[Event]
          _ <- eventRepository.create(event)
          response <- Created()
        } yield response
    }

    routes.orNotFound
  }

  val serve: RIO[R with Clock, Unit] = {
    type IO[A] = RIO[R with Clock, A]
    import zio.interop.catz._
    ZIO.runtime[R with Clock].flatMap { implicit clock =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(CORS(routes))
        .serve
        .compile[IO, IO, ExitCode]
        .drain
    }
  }
}
