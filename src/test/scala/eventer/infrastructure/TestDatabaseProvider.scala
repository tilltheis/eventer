package eventer.infrastructure

import eventer.infrastructure.DatabaseProvider.{Service, Simple}
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{RManaged, Task}

object TestDatabaseProvider {
  trait WithDroppedSchemaAndMigration extends Simple {
    override def databaseProvider: Service = new Service {
      override def database: RManaged[Blocking, DatabaseContext] =
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
          zio.blocking.blocking(Task(migrate())).map(_ => db)
        }
    }
  }
}
