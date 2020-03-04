package eventer.infrastructure

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import io.getquill.{Escape, NamingStrategy, PostgresJdbcContext, SnakeCase}
import zio.RIO
import zio.blocking.Blocking

object DatabaseContext {

  class Service(quillConfigKey: String) extends PostgresJdbcContext(NamingStrategy(SnakeCase, Escape), quillConfigKey) {
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
  }

}
