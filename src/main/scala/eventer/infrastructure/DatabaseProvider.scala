package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{RManaged, Task, ZManaged}

trait DatabaseProvider {
  def databaseProvider: DatabaseProvider.Service
}

object DatabaseProvider {
  trait Service {
    def database: RManaged[Blocking, DatabaseContext]
  }

  class Simple(quillConfigKey: String) extends DatabaseProvider {
    override def databaseProvider: Service = new Service {
      override val database: RManaged[Blocking, DatabaseContext] =
        ZManaged
          .fromAutoCloseable(Task(new DatabaseContext.Service(quillConfigKey) {}))
          .map(new DatabaseContext.Live(_))
    }
  }

  class WithMigration(quillConfigKey: String) extends Simple(quillConfigKey) {
    override val databaseProvider: Service = new Service {
      override val database: RManaged[Blocking, DatabaseContext] =
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
}
