package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{UIO, ZLayer}

object TestDatabaseProvider {
  def withDroppedSchemaAndMigration(quillConfigKey: String): ZLayer[Blocking, Nothing, DatabaseProvider] = {
    val dbctx: ZLayer[Blocking with DatabaseProvider, Nothing, DatabaseProvider] =
      ZLayer.fromServiceM[DatabaseProvider.Service, Blocking, Nothing, DatabaseProvider.Service] { db =>
        def migrate() = {
          val flyway = Flyway
            .configure()
            .locations("classpath:migration")
            .dataSource(db.database.get.dataSource)
            .load()

          flyway.clean()
          flyway.migrate()
        }

        zio.blocking.blocking(UIO(migrate())).map(_ => db)
      }

    (ZLayer.requires[Blocking] ++ DatabaseProvider.simple(quillConfigKey)) >>> dbctx
  }
}
