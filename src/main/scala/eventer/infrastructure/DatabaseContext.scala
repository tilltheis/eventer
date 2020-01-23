package eventer.infrastructure

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import eventer.infrastructure.DatabaseContext.Service
import io.getquill.{PostgresJdbcContext, SnakeCase}
import zio.Task
import zio.blocking.Blocking

trait DatabaseContext {
  def databaseContext: Service
}

object DatabaseContext {

  class Service(blocking: Blocking) extends PostgresJdbcContext[SnakeCase](SnakeCase, "quill") {
    object schema {
      val event: Quoted[EntityQuery[DbEvent]] = quote(
        querySchema[DbEvent]("event", _.instant -> "date_time", _.zoneId -> "time_zone"))
    }

    implicit val instantEncoder: MappedEncoding[Instant, LocalDateTime] = MappedEncoding(
      LocalDateTime.ofInstant(_, ZoneOffset.UTC))
    implicit val instantDecoder: MappedEncoding[LocalDateTime, Instant] = MappedEncoding(_.toInstant(ZoneOffset.UTC))
    implicit val zoneIdEncoder: MappedEncoding[ZoneId, String] = MappedEncoding(_.getId)
    implicit val zoneIdDecoder: MappedEncoding[String, ZoneId] = MappedEncoding(ZoneId.of)

    def performEffect[T](io: IO[T, _]): Task[Result[T]] =
      zio.blocking.blocking(Task(performIO(io))).provide(blocking)

    def performEffect_(io: IO[_, _]): Task[Result[Unit]] =
      performEffect(io).map(_ => ())
  }

}
