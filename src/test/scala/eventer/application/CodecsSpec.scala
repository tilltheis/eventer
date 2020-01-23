package eventer.application

import java.time.format.DateTimeFormatter

import eventer.domain.{Event, TestData}
import io.circe.Json
import zio.Task
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import io.circe.syntax.EncoderOps

object CodecsSpec {
  val codecs = new Codecs[Task]
  import codecs._

  val json = Json.obj(
    "id" -> TestData.event.id.id.toString.asJson,
    "title" -> TestData.event.title.asJson,
    "description" -> TestData.event.description.asJson,
    "host" -> TestData.event.host.asJson,
    "dateTime" -> TestData.event.dateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME).asJson,
    "createdAt" -> TestData.event.createdAt.format(DateTimeFormatter.ISO_DATE_TIME).asJson,
    "updatedAt" -> TestData.event.createdAt.format(DateTimeFormatter.ISO_DATE_TIME).asJson
  )

  val spec = suite("Codecs") {
    suite("Event")(
      suite("encode")(test("works") {
        assert(TestData.event.asJson, equalTo(json))
      }),
      suite("decode")(test("works") {
        assert(json.as[Event], isRight(equalTo(TestData.event)))
      })
    )
  }
}
