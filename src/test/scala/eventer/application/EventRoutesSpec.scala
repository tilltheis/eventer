package eventer.application

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.instances.option._
import cats.syntax.traverse._
import eventer.TestEnvSpec
import eventer.domain.{Event, InMemoryEventRepository, SessionUser, TestData}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRequest, EntityDecoder, Method, Request, Response, Status}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.{AuthMiddleware, Middleware}
import org.http4s.syntax.kleisli._
import zio.interop.catz._
import zio.{Has, RIO, UIO, ZIO}
import zio.test._
import zio.test.Assertion._

object EventRoutesSpec extends DefaultRunnableSpec {

  val alwaysAuthedMiddleware = new Middlewares[Has[InMemoryEventRepository.State]] {
    override def auth[R]: Middleware[OptionT[RIO[R with Has[InMemoryEventRepository.State], *], *],
                                     AuthedRequest[RIO[R with Has[InMemoryEventRepository.State], *], SessionUser],
                                     Response[RIO[R with Has[InMemoryEventRepository.State], *]],
                                     Request[RIO[R with Has[InMemoryEventRepository.State], *]],
                                     Response[RIO[R with Has[InMemoryEventRepository.State], *]]] = {
      val innerDsl = Http4sDsl[RIO[R with Has[InMemoryEventRepository.State], *]]
      import innerDsl._
      AuthMiddleware.noSpider(Kleisli(_ => OptionT.some(TestData.sessionUser)), _ => Forbidden())
    }
  }

  val neverAuthedMiddleware = new Middlewares[Has[InMemoryEventRepository.State]] {
    override def auth[R]: Middleware[OptionT[RIO[R with Has[InMemoryEventRepository.State], *], *],
                                     AuthedRequest[RIO[R with Has[InMemoryEventRepository.State], *], SessionUser],
                                     Response[RIO[R with Has[InMemoryEventRepository.State], *]],
                                     Request[RIO[R with Has[InMemoryEventRepository.State], *]],
                                     Response[RIO[R with Has[InMemoryEventRepository.State], *]]] = {
      val innerDsl = Http4sDsl[RIO[R with Has[InMemoryEventRepository.State], *]]
      import innerDsl._
      AuthMiddleware.noSpider(Kleisli(_ => OptionT.none), _ => Forbidden())
    }
  }

  type IO[A] = RIO[Has[InMemoryEventRepository.State], A]

  val codecs = Codecs[IO]
  import codecs._

  def parseResponseBody[A](response: Response[IO])(implicit F: MonadError[IO, Throwable],
                                                   decoder: EntityDecoder[IO, A]): IO[Option[Either[Throwable, A]]] =
    response.body.compile.toVector.flatMap { bytes =>
      Option
        .when(bytes.nonEmpty)(response)
        .traverse(_.as[A](F, decoder).map(Right(_)).catchAll(t => UIO.left(t)))
    }

  val spec: TestEnvSpec = suite("EventRoutes")(
    suite("GET /")(testM("returns the events from the repository") {

      val routes = new EventRoutes[Has[InMemoryEventRepository.State], Has[InMemoryEventRepository.State]](
        new InMemoryEventRepository,
        UIO.succeed(TestData.eventId),
        neverAuthedMiddleware)

      val events = Seq(TestData.event, TestData.event)
      val responseM = routes.routes.orNotFound.run(Request(Method.GET, uri"/"))
      (
        for {
          response <- responseM
          body <- parseResponseBody[Seq[Event]](response)
        } yield
          assert(response.status)(equalTo(Status.Ok)) &&
            assert(body)(isSome(isRight(equalTo(events))))
      ).provideLayer(InMemoryEventRepository.State.make(events).toLayer)
    }),
    suite("POST /")(
      testM("inserts the event into the repository") {
        val routes = new EventRoutes[Has[InMemoryEventRepository.State], Has[InMemoryEventRepository.State]](
          new InMemoryEventRepository,
          UIO.succeed(TestData.eventId),
          alwaysAuthedMiddleware)
        val request = Request(Method.POST, uri"/").withEntity(TestData.eventCreationRequest)
        val responseM = routes.routes.orNotFound.run(request)

        (
          for {
            response <- responseM
            body <- parseResponseBody[Unit](response)
            finalState <- ZIO.accessM[Has[InMemoryEventRepository.State]](_.get.stateRef.get)
          } yield
            assert(response.status)(equalTo(Status.Created)) &&
              assert(body)(isNone) &&
              assert(finalState)(equalTo(Seq(TestData.event)))
        ).provideLayer(InMemoryEventRepository.State.empty.toLayer)
      },
      testM("rejects requests that are not authenticated") {
        val routes = new EventRoutes[Has[InMemoryEventRepository.State], Has[InMemoryEventRepository.State]](
          new InMemoryEventRepository,
          UIO.succeed(TestData.eventId),
          neverAuthedMiddleware)
        val request = Request(Method.POST, uri"/").withEntity(TestData.event)
        val responseM = routes.routes.orNotFound.run(request)
        (
          for {
            response <- responseM
          } yield assert(response.status)(equalTo(Status.Forbidden))
        ).provideLayer(InMemoryEventRepository.State.empty.toLayer)
      }
    )
  )
}
