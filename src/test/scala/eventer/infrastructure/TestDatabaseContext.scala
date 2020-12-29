package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{UIO, ZLayer}

object TestDatabaseContext {
  def withDroppedSchemaAndMigration: ZLayer[Blocking with DatabaseProvider, Nothing, DatabaseContext] = {
    ZLayer.fromServiceM[DatabaseProvider.Service, Blocking, Nothing, DatabaseContext.Service] { db =>
      def migrate() = {
        val flyway = Flyway
          .configure()
          .locations("classpath:migration")
          .dataSource(db.database.get.dataSource)
          .load()

        val _ = flyway.clean()
        flyway.migrate()
      }

      zio.blocking.blocking(UIO(migrate())).as(db.database.get)
    }
  }
}
