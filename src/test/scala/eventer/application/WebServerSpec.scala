package eventer.application

import eventer.domain.{Event, InMemoryEventRepository, TestData}
import org.http4s._
import zio.Task
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object WebServerSpec {
  val spec = suite("WebServer")(
    suite("GET /events")(testM("returns the events from the repository") {
      val events = Seq(TestData.event, TestData.event)
      val eventRepository = InMemoryEventRepository.make(events)
      val webServer = new WebServer[Any](eventRepository)
      val responseM = webServer.routes[Any].run(Request(Method.GET, Uri.uri("/events")))

      val codecs = new Codecs[Task]
      import codecs._

      for {
        response <- responseM
        responseEvents <- response.as[Seq[Event]]
      } yield {
        assert[(Response[Task], Seq[Event])](
          (response, responseEvents),
          hasField("status", (r: (Response[Task], Seq[Event])) => r._1.status, equalTo(Status.Ok)) &&
            hasField("body", (r: (Response[Task], Seq[Event])) => r._2, equalTo(events))
        )
      }
    }),
    suite("POST /events")(testM("inserts the event into the repository") {
      val codecs = new Codecs[Task]
      import codecs._

      val eventRepository = InMemoryEventRepository.empty
      val webServer = new WebServer[Any](eventRepository)
      val responseM = webServer.routes[Any].run(Request(Method.POST, Uri.uri("/events")).withEntity(TestData.event))

      for {
        response <- responseM
        body <- response.body.compile.toVector
      } yield {
        assert[(Response[Task], Seq[Byte])](
          (response, body),
          hasField("status", (r: (Response[Task], Seq[Byte])) => r._1.status, equalTo(Status.Created)) &&
            hasField("body", (r: (Response[Task], Seq[Byte])) => r._2, isEmpty)
        )
      }
    })
  )
}
