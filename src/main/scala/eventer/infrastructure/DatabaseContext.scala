package eventer.infrastructure

import io.getquill._
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{Task, UIO, URLayer, ZLayer}

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

    def performEffect[T](io: IO[T, _]): Task[Result[T]] =
      blocking.blocking(Task(performIO(io)))

    def performEffect_(io: IO[_, _]): Task[Result[Unit]] =
      performEffect(io).unit
  }

  val withMigration: URLayer[Blocking with DatabaseProvider, DatabaseContext] = {
    ZLayer.fromServiceM[DatabaseProvider.Service, Blocking, Nothing, DatabaseContext.Service] { db =>
      def migrate() =
        Flyway
          .configure()
          .locations("classpath:migration")
          .dataSource(db.database.get.dataSource)
          .load()
          .migrate()

      zio.blocking.blocking(UIO(migrate())).as(db.database.get)
    }
  }

}
