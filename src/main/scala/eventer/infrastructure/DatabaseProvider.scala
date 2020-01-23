package eventer.infrastructure

import eventer.infrastructure
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{RManaged, Task, ZIO, ZManaged}

trait DatabaseProvider {
  def databaseProvider: DatabaseProvider.Service
}

object DatabaseProvider {
  trait Service {
    def database: RManaged[Blocking, DatabaseContext]
  }

  trait Simple extends DatabaseProvider {
    override def databaseProvider: Service = new Service {
      override def database: RManaged[Blocking, DatabaseContext] =
        ZManaged
          .fromAutoCloseable(
            zio.blocking.blocking(ZIO.environment[Blocking].map(new infrastructure.DatabaseContext.Service(_))))
          .map { ctx =>
            new DatabaseContext {
              override def databaseContext: DatabaseContext.Service = ctx
            }
          }
    }
  }

  trait WithMigration extends Simple {
    override def databaseProvider: Service = new Service {
      override def database: RManaged[Blocking, DatabaseContext] =
        WithMigration.super.databaseProvider.database.mapM { db =>
          def migrate() =
            Flyway
              .configure()
              .locations("classpath:migration")
              .dataSource(db.databaseContext.dataSource)
              .load()
              .migrate()
          zio.blocking.blocking(Task(migrate())).map(_ => db)
        }
    }
  }

  trait Live extends WithMigration

  object Live extends Live
}
