package eventer.application

import cats.effect.ExitCode
import com.typesafe.scalalogging.StrictLogging
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{HttpMiddleware, Router}
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import zio.interop.catz._
import zio.{Task, UIO, ZIO}

import scala.concurrent.ExecutionContext

class WebServer(eventRoutes: HttpRoutes[Task],
                sessionRoutes: HttpRoutes[Task],
                userRoutes: HttpRoutes[Task],
                csrfMiddleware: HttpMiddleware[Task])
    extends StrictLogging
    with Http4sDsl[Task]
    with Codecs[Task] {

  private[application] val routes: HttpApp[Task] = {
    val allRoutes =
      Router("/events" -> eventRoutes, "/sessions" -> sessionRoutes, "/users" -> userRoutes)
    csrfMiddleware(allRoutes).orNotFound
  }

  def serve(port: Int): Task[Unit] = {
    import zio.interop.catz.implicits._
    ZIO.runtime[Any].flatMap { implicit rts =>
      BlazeServerBuilder[Task](ExecutionContext.global)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(routes)
        .withServiceErrorHandler(request => {
          case throwable =>
            logger.error(s"Error handling request ${request.method.toString} ${request.uri.toString}", throwable)
            UIO.succeed(Response(Status.InternalServerError))
        })
        .serve
        .compile[Task, Task, ExitCode]
        .drain
    }
  }
}
