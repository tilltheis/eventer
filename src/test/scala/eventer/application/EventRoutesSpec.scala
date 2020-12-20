package eventer.application

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.instances.option._
import cats.syntax.traverse._
import eventer.TestEnvSpec
import eventer.domain.event.InMemoryEventRepository2
import eventer.domain.{Event, SessionUser, TestData}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.{AuthMiddleware, Middleware}
import org.http4s.syntax.kleisli._
import org.http4s._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.{Task, UIO}

object EventRoutesSpec extends DefaultRunnableSpec with Http4sDsl[Task] with Codecs[Task] {

  val alwaysAuthedMiddleware = new Middlewares {
    override val auth
      : Middleware[OptionT[Task, *], AuthedRequest[Task, SessionUser], Response[Task], Request[Task], Response[Task]] =
      AuthMiddleware.noSpider(Kleisli(_ => OptionT.some(TestData.sessionUser)), _ => Forbidden())
  }

  val neverAuthedMiddleware = new Middlewares {
    override val auth
      : Middleware[OptionT[Task, *], AuthedRequest[Task, SessionUser], Response[Task], Request[Task], Response[Task]] =
      AuthMiddleware.noSpider(Kleisli(_ => OptionT.none), _ => Forbidden())
  }

  def parseResponseBody[A](response: Response[Task])(
      implicit F: MonadError[Task, Throwable],
      decoder: EntityDecoder[Task, A]): Task[Option[Either[Throwable, A]]] =
    response.body.compile.toVector.flatMap { bytes =>
      Option
        .when(bytes.nonEmpty)(response)
        .traverse(_.as[A](F, decoder).map(Right(_)).catchAll(t => UIO.left(t)))
    }

  val spec: TestEnvSpec = suite("EventRoutes")(
    suite("GET /")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      for {
        repository <- InMemoryEventRepository2.make(events)
        routes = new EventRoutes(repository, UIO.succeed(TestData.eventId), neverAuthedMiddleware)
        response <- routes.routes.run(Request(Method.GET, uri"/")).value.someOrFailException
        body <- parseResponseBody[Seq[Event]](response)
      } yield
        assert(response.status)(equalTo(Status.Ok)) &&
          assert(body)(isSome(isRight(equalTo(events))))
    }),
    suite("POST /")(
      testM("inserts the event into the repository") {
        for {
          repository <- InMemoryEventRepository2.empty
          routes = new EventRoutes(repository, UIO.succeed(TestData.eventId), alwaysAuthedMiddleware)
          request = Request(Method.POST, uri"/").withEntity(TestData.eventCreationRequest)
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
          finalState <- repository.findAll
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(body)(isNone) &&
            assert(finalState)(equalTo(Seq(TestData.event)))
      },
      testM("rejects requests that are not authenticated") {
        for {
          repository <- InMemoryEventRepository2.empty
          routes = new EventRoutes(repository, UIO.succeed(TestData.eventId), neverAuthedMiddleware)
          request = Request(Method.POST, uri"/").withEntity(TestData.event)
          response <- routes.routes.run(request).value.someOrFailException
        } yield assert(response.status)(equalTo(Status.Forbidden))
      }
    )
  )
}
