package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{UIO, URLayer, ZLayer}

object TestDatabaseProvider {
  def withDroppedSchemaAndMigration(quillConfigKey: String): ZLayer[Blocking, Nothing, DatabaseProvider] = {
    val dbCtx: URLayer[Blocking with DatabaseProvider, DatabaseProvider] =
      ZLayer.fromServiceM[DatabaseProvider.Service, Blocking, Nothing, DatabaseProvider.Service] { db =>
        def migrate() = {
          val flyway = Flyway
            .configure()
            .locations("classpath:migration")
            .dataSource(db.database.get.dataSource)
            .load()

          val _ = flyway.clean()
          flyway.migrate()
        }

        zio.blocking.blocking(UIO(migrate())).as(db)
      }

    (ZLayer.requires[Blocking] ++ DatabaseProvider.simple(quillConfigKey)) >>> dbCtx
  }
}
