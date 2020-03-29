package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{Has, UIO, ULayer, ZLayer, ZManaged}

object DatabaseProvider {
  trait Service {
    def database: DatabaseContext
  }

  def simple(quillConfigKey: String): ULayer[DatabaseProvider] =
    ZLayer.fromManaged(
      ZManaged
        .fromAutoCloseable(UIO(new DatabaseContext.Service(quillConfigKey)))
        .map(ctx =>
          new Service {
            override def database: DatabaseContext = Has(ctx)
        }))

  def withMigration(quillConfigKey: String): ZLayer[Blocking, Nothing, DatabaseProvider] = {
    val dbCtx: ZLayer[Blocking with DatabaseProvider, Nothing, DatabaseProvider] =
      ZLayer.fromServiceM[DatabaseProvider.Service, Blocking, Nothing, DatabaseProvider.Service] { db =>
        def migrate() =
          Flyway
            .configure()
            .locations("classpath:migration")
            .dataSource(db.database.get.dataSource)
            .load()
            .migrate()

        zio.blocking.blocking(UIO(migrate())).as(db)
      }

    (ZLayer.requires[Blocking] ++ simple(quillConfigKey)) >>> dbCtx
  }
}
