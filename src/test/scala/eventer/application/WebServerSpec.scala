package eventer.application

import eventer.UnitSpec
import eventer.domain.{Event, TestData}
import eventer.infrastructure.InMemoryEventRepository
import eventer.infrastructure.InMemoryEventRepository.State
import org.http4s._
import zio.clock.Clock
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, URIO, ZIO}

object WebServerSpec {
  val spec: UnitSpec = suite("WebServer")(
    suite("GET /events")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      val eventRepository = new InMemoryEventRepository
      val webServer = new WebServer(eventRepository)
      val responseM = webServer.routes.run(Request(Method.GET, Uri.uri("/events")))

      val codecs = new Codecs[webServer.IO]
      import codecs._

      val stateM = InMemoryEventRepository.makeState(events)
      for {
        response <- responseM.provideSomeM(envWithStateM(stateM))
        responseEvents <- response.as[Seq[Event]].provideSomeM(envWithStateM(stateM))
      } yield {
        assert[(Response[webServer.IO], Seq[Event])](
          (response, responseEvents),
          hasField("status", (r: (Response[webServer.IO], Seq[Event])) => r._1.status, equalTo(Status.Ok)) &&
            hasField("body", (r: (Response[webServer.IO], Seq[Event])) => r._2, equalTo(events))
        )
      }
    }),
    suite("POST /events")(testM("inserts the event into the repository") {
      val eventRepository = new InMemoryEventRepository
      val webServer = new WebServer(eventRepository)
      val codecs = new Codecs[webServer.IO]
      import codecs._

      val responseM = webServer.routes.run(Request(Method.POST, Uri.uri("/events")).withEntity(TestData.event))

      val stateM = InMemoryEventRepository.emptyState
      for {
        response <- responseM.provideSomeM(envWithStateM(stateM))
        body <- response.body.compile.toVector.provideSomeM(envWithStateM(stateM))
      } yield {
        assert[(Response[webServer.IO], Seq[Byte])](
          (response, body),
          hasField("status", (r: (Response[webServer.IO], Seq[Byte])) => r._1.status, equalTo(Status.Created)) &&
            hasField("body", (r: (Response[webServer.IO], Seq[Byte])) => r._2, isEmpty)
        )
      }
    })
  )

  def envWithStateM(stateM: UIO[State]): URIO[Clock, State with Clock] =
    for {
      state <- stateM
      envClock <- ZIO.environment[Clock]
    } yield
      new State with Clock {
        override def stateRef: Ref[Seq[Event]] = state.stateRef
        override val clock: Clock.Service[Any] = envClock.clock
      }
}
