package eventer.infrastructure

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

  trait WithoutMigration extends DatabaseProvider {
    override def databaseProvider: Service = new Service {
      override def database: RManaged[Blocking, DatabaseContext] =
        ZManaged.fromAutoCloseable(zio.blocking.blocking(ZIO.environment[Blocking].map(new DatabaseContext(_))))
    }
  }

  trait WithMigration extends WithoutMigration {
    override def databaseProvider: Service = new Service {
      override def database: RManaged[Blocking, DatabaseContext] =
        WithMigration.super.databaseProvider.database.mapM { db =>
          def migrate() = Flyway.configure().locations("classpath:migration").dataSource(db.dataSource).load().migrate()
          zio.blocking.blocking(Task(migrate())).map(_ => db)
        }
    }
  }

  trait Live extends WithMigration

  object Live extends Live
}
