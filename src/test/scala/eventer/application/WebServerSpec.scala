package eventer.application

import eventer.UnitSpec
import eventer.domain.{Event, TestData}
import eventer.infrastructure.InMemoryEventRepository
import org.http4s._
import zio.RIO
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object WebServerSpec {
  val spec: UnitSpec = suite("WebServer")(
    suite("GET /events")(testM("returns the events from the repository") {
      type IO[A] = ({ type T[A] = RIO[InMemoryEventRepository.State, A] })#T[A]
      val codecs = new Codecs[IO]
      import codecs._

      val events = Seq(TestData.event, TestData.event)
      val eventRepository = new InMemoryEventRepository
      val webServer = new WebServer(eventRepository)
      val responseM = webServer.routes[InMemoryEventRepository.State].run(Request(Method.GET, Uri.uri("/events")))

      for {
        state <- InMemoryEventRepository.makeState(events)
        response <- responseM.provide(state)
        responseEvents <- response.as[Seq[Event]].provide(state)
      } yield {
        assert[(Response[IO], Seq[Event])](
          (response, responseEvents),
          hasField("status", (r: (Response[IO], Seq[Event])) => r._1.status, equalTo(Status.Ok)) &&
            hasField("body", (r: (Response[IO], Seq[Event])) => r._2, equalTo(events))
        )
      }
    }),
    suite("POST /events")(testM("inserts the event into the repository") {
      type IO[A] = ({ type T[A] = RIO[InMemoryEventRepository.State, A] })#T[A]
      val codecs = new Codecs[IO]
      import codecs._

      val eventRepository = new InMemoryEventRepository
      val webServer = new WebServer(eventRepository)
      val responseM = webServer
        .routes[InMemoryEventRepository.State]
        .run(Request(Method.POST, Uri.uri("/events")).withEntity(TestData.event))

      for {
        state <- InMemoryEventRepository.emptyState
        response <- responseM.provide(state)
        body <- response.body.compile.toVector.provide(state)
      } yield {
        assert[(Response[IO], Seq[Byte])](
          (response, body),
          hasField("status", (r: (Response[IO], Seq[Byte])) => r._1.status, equalTo(Status.Created)) &&
            hasField("body", (r: (Response[IO], Seq[Byte])) => r._2, isEmpty)
        )
      }
    })
  )
}
