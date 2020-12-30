package eventer.application

import eventer.domain.event.EventRepository
import eventer.domain.{Event, TestData}
import eventer.infrastructure.test.InMemoryEventRepository
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.{UIO, ULayer, ZIO, ZLayer}

object EventRoutesSpec extends RoutesSpec {
  private val eventIdGeneratorLayer: ULayer[EventIdGenerator] = ZLayer.succeed(UIO.succeed(TestData.eventId))

  val spec: TestEnvSpec = suite("EventRoutes")(
    suite("GET /")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      (
        for {
          _ <- InMemoryEventRepository.createAll(events: _*)
          routes <- EventRoutes.routes
          response <- routes.run(Request(Method.GET, uri"/")).value.someOrFailException
          body <- parseResponseBody[Seq[Event]](response)
        } yield
          assert(response.status)(equalTo(Status.Ok)) &&
            assert(body)(isSome(isRight(equalTo(events))))
      ).provideCustomLayer(
        InMemoryEventRepository.empty >+> eventIdGeneratorLayer >+> neverAuthedMiddlewareLayer >+> EventRoutes.live)
    }),
    suite("POST /")(
      testM("inserts the event into the repository") {
        (
          for {
            repository <- ZIO.service[EventRepository.Service]
            routes <- EventRoutes.routes
            request = Request(Method.POST, uri"/").withEntity(TestData.eventCreationRequest)
            response <- routes.run(request).value.someOrFailException
            body <- parseResponseBody[Unit](response)
            finalState <- repository.findAll
          } yield
            assert(response.status)(equalTo(Status.Created)) &&
              assert(body)(isNone) &&
              assert(finalState)(equalTo(Seq(TestData.event)))
        ).provideCustomLayer(
          InMemoryEventRepository.empty >+> eventIdGeneratorLayer >+> alwaysAuthedMiddlewareLayer >+> EventRoutes.live)
      },
      testM("rejects requests that are not authenticated") {
        (
          for {
            routes <- EventRoutes.routes
            request = Request(Method.POST, uri"/").withEntity(TestData.event)
            response <- routes.run(request).value.someOrFailException
          } yield assert(response.status)(equalTo(Status.Forbidden))
        ).provideCustomLayer(
          InMemoryEventRepository.empty >+> eventIdGeneratorLayer >+> neverAuthedMiddlewareLayer >+> EventRoutes.live)
      }
    )
  )
}
