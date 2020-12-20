package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{Has, RLayer, UIO, ULayer, URLayer, ZLayer, ZManaged}

object DatabaseProvider {
  trait Service {
    def database: DatabaseContext
  }

  def simple(quillConfigKey: String): URLayer[Blocking, DatabaseProvider] =
    ZLayer.fromServiceManaged(
      blocking =>
        ZManaged
          .fromAutoCloseable(UIO(new DatabaseContext.Service(quillConfigKey, blocking)))
          .map(ctx =>
            new Service {
              override def database: DatabaseContext = Has(ctx)
          }))

  def withMigration(quillConfigKey: String): URLayer[Blocking, DatabaseProvider] = {
    val dbCtx: URLayer[Blocking with DatabaseProvider, DatabaseProvider] =
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

    (ZLayer.requires[Blocking] ++ (ZLayer.requires[Blocking] >>> simple(quillConfigKey))) >>> dbCtx
  }
}
