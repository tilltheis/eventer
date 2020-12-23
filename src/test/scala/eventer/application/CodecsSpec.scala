package eventer.application

import eventer.EventerSpec
import eventer.domain.{Event, TestData}
import io.circe.Json
import io.circe.syntax.EncoderOps
import zio.Task
import zio.test.Assertion._
import zio.test._

import java.time.format.DateTimeFormatter

object CodecsSpec extends EventerSpec {
  val codecs = Codecs[Task]
  import codecs._

  val json = Json.obj(
    "id" -> TestData.event.id.id.toString.asJson,
    "title" -> TestData.event.title.asJson,
    "description" -> TestData.event.description.asJson,
    "hostId" -> TestData.event.hostId.asJson,
    "dateTime" -> TestData.event.dateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME).asJson
  )

  val spec: TestEnvSpec = suite("Codecs") {
    suite("Event")(
      suite("encode")(test("works") {
        assert(TestData.event.asJson)(equalTo(json))
      }),
      suite("decode")(test("works") {
        assert(json.as[Event])(isRight(equalTo(TestData.event)))
      })
    )
  }
}
