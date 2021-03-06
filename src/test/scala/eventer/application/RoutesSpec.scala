package eventer.application

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.instances.option._
import cats.syntax.traverse._
import eventer.EventerSpec
import eventer.domain.TestData
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware => Http4sAuthMiddleware, HttpMiddleware}
import org.http4s.{EntityDecoder, Response}
import zio.interop.catz._
import zio.{Task, UIO, ULayer, ZLayer}

abstract class RoutesSpec extends EventerSpec with Http4sDsl[Task] with Codecs[Task] {
  val alwaysVerifiedCsrfMiddleware: HttpMiddleware[Task] = identity

  val neverVerifiedCsrfMiddleware: HttpMiddleware[Task] = Function.const(Kleisli.pure(Response(Forbidden)))

  val alwaysAuthedMiddleware: AuthMiddleware.Service =
    Http4sAuthMiddleware.noSpider(Kleisli(_ => OptionT.some(TestData.sessionUser)), _ => Forbidden())

  val neverAuthedMiddleware: AuthMiddleware.Service =
    Http4sAuthMiddleware.noSpider(Kleisli(_ => OptionT.none), _ => Forbidden())

  def parseResponseBody[A](response: Response[Task])(
      implicit F: MonadError[Task, Throwable],
      decoder: EntityDecoder[Task, A]): Task[Option[Either[Throwable, A]]] =
    response.body.compile.toVector.flatMap { bytes =>
      Option
        .when(bytes.nonEmpty)(response)
        .traverse(_.as[A](F, decoder).map(Right(_)).catchAll(t => UIO.left(t)))
    }

  val neverAuthedMiddlewareLayer: ULayer[AuthMiddleware] = ZLayer.succeed(neverAuthedMiddleware)
  val alwaysAuthedMiddlewareLayer: ULayer[AuthMiddleware] = ZLayer.succeed(alwaysAuthedMiddleware)
}
