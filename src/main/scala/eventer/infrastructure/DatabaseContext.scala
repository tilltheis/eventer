package eventer.infrastructure

import io.getquill._
import zio.blocking.Blocking
import zio.{RIO, Task}

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

object DatabaseContext {

  class Service(quillConfigKey: String, blocking: Blocking.Service)
      extends PostgresJdbcContext(NamingStrategy(SnakeCase, Escape), quillConfigKey) {
    object schema {
      val event: Quoted[EntityQuery[DbEvent]] = quote(
        querySchema[DbEvent]("event", _.instant -> "date_time", _.zoneId -> "time_zone"))
    }

    implicit val instantEncoder: MappedEncoding[Instant, LocalDateTime] = MappedEncoding(
      LocalDateTime.ofInstant(_, ZoneOffset.UTC))
    implicit val instantDecoder: MappedEncoding[LocalDateTime, Instant] = MappedEncoding(_.toInstant(ZoneOffset.UTC))
    implicit val zoneIdEncoder: MappedEncoding[ZoneId, String] = MappedEncoding(_.getId)
    implicit val zoneIdDecoder: MappedEncoding[String, ZoneId] = MappedEncoding(ZoneId.of)

    def performEffect[T](io: IO[T, _]): RIO[Blocking, Result[T]] =
      zio.blocking.blocking(RIO(performIO(io)))

    def performEffect_(io: IO[_, _]): RIO[Blocking, Result[Unit]] =
      performEffect(io).unit

    def performEffect2[T](io: IO[T, _]): Task[Result[T]] =
      blocking.blocking(Task(performIO(io)))

    def performEffect_2(io: IO[_, _]): Task[Result[Unit]] =
      performEffect2(io).unit
  }

}
