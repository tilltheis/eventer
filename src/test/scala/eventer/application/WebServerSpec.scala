package eventer.application

import eventer.domain.{Event, TestData, TestEventRepository}
import org.http4s.{Request, Response, Status, Uri}
import zio.RIO
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object WebServerSpec {
  val spec = suite("WebServer") {
    suite("GET /events") {
      testM("returns the events from the repository") {
        val events = Seq(TestData.event, TestData.event)
        val eventRepository = new TestEventRepository[Any] {
          override def findAll: RIO[Any, Seq[Event]] = RIO.succeed(events)
        }
        val webServer = new WebServer[Any](eventRepository)
        val responseM = webServer.routes.run(Request(uri = Uri.uri("/events")))

        val codecs = new Codecs[webServer.IO]
        import codecs._

        for {
          response <- responseM
          responseEvents <- response.as[Seq[Event]]
        } yield {
          assert[(Response[webServer.IO], Seq[Event])](
            (response, responseEvents),
            hasField("status", (r: (Response[webServer.IO], Seq[Event])) => r._1.status, equalTo(Status.Ok)) &&
              hasField("body", (r: (Response[webServer.IO], Seq[Event])) => r._2, equalTo(events))
          )
        }
      }
    }
  }
}
