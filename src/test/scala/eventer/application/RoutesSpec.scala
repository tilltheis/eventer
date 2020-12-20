package eventer.application

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.instances.option._
import cats.syntax.traverse._
import eventer.domain.{SessionUser, TestData}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{EntityDecoder, Response}
import zio.interop.catz._
import zio.test.DefaultRunnableSpec
import zio.{Task, UIO}

abstract class RoutesSpec extends DefaultRunnableSpec with Http4sDsl[Task] with Codecs[Task] {
  val alwaysAuthedMiddleware: AuthMiddleware[Task, SessionUser] =
    AuthMiddleware.noSpider(Kleisli(_ => OptionT.some(TestData.sessionUser)), _ => Forbidden())

  val neverAuthedMiddleware: AuthMiddleware[Task, SessionUser] =
    AuthMiddleware.noSpider(Kleisli(_ => OptionT.none), _ => Forbidden())

  def parseResponseBody[A](response: Response[Task])(
      implicit F: MonadError[Task, Throwable],
      decoder: EntityDecoder[Task, A]): Task[Option[Either[Throwable, A]]] =
    response.body.compile.toVector.flatMap { bytes =>
      Option
        .when(bytes.nonEmpty)(response)
        .traverse(_.as[A](F, decoder).map(Right(_)).catchAll(t => UIO.left(t)))
    }
}
