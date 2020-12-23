package eventer.application

import eventer.TestEnvSpec
import eventer.domain.{Event, TestData}
import eventer.infrastructure.TestEventRepository
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import zio.UIO
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object EventRoutesSpec extends RoutesSpec {
  val spec: TestEnvSpec = suite("EventRoutes")(
    suite("GET /")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      for {
        repository <- TestEventRepository.make(events)
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
          repository <- TestEventRepository.empty
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
          repository <- TestEventRepository.empty
          routes = new EventRoutes(repository, UIO.succeed(TestData.eventId), neverAuthedMiddleware)
          request = Request(Method.POST, uri"/").withEntity(TestData.event)
          response <- routes.routes.run(request).value.someOrFailException
        } yield assert(response.status)(equalTo(Status.Forbidden))
      }
    )
  )
}
