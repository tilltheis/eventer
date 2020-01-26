package eventer.application

import eventer.UnitSpec
import eventer.domain.{Event, TestData}
import eventer.infrastructure.InMemoryEventRepository
import org.http4s._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.RIO

object WebServerSpec {
  val spec: UnitSpec = suite("WebServer")(
    suite("GET /events")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      val eventRepository = new InMemoryEventRepository
      val webServer = new WebServer(eventRepository)
      val responseM = webServer.routes[InMemoryEventRepository.State].run(Request(Method.GET, Uri.uri("/events")))

      type IO[A] = ({ type T[A] = RIO[InMemoryEventRepository.State, A] })#T[A]
      val codecs = new Codecs[IO]
      import codecs._

      val state = InMemoryEventRepository.makeState(events)
      for {
        response <- responseM.provideM(state)
        responseEvents <- response.as[Seq[Event]].provideM(state)
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

      val state = InMemoryEventRepository.emptyState
      for {
        response <- responseM.provideM(state)
        body <- response.body.compile.toVector.provideM(state)
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
