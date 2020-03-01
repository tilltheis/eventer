package eventer.infrastructure

import eventer.infrastructure.DatabaseProvider.{Service, Simple}
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{UIO, URManaged}

object TestDatabaseProvider {
  class WithDroppedSchemaAndMigration(quillConfigKey: String) extends Simple(quillConfigKey) {
    override def databaseProvider: Service = new Service {
      override def database: URManaged[Blocking, DatabaseContext] =
        WithDroppedSchemaAndMigration.super.databaseProvider.database.mapM { db =>
          def migrate() = {
            val flyway = Flyway
              .configure()
              .locations("classpath:migration")
              .dataSource(db.databaseContext.dataSource)
              .load()

            flyway.clean()
            flyway.migrate()
          }
          zio.blocking.blocking(UIO(migrate())).map(_ => db)
        }
    }
  }
}
