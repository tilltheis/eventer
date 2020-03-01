package eventer.infrastructure

import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{UIO, URManaged, ZManaged}

trait DatabaseProvider {
  def databaseProvider: DatabaseProvider.Service
}

object DatabaseProvider {
  trait Service {
    def database: URManaged[Blocking, DatabaseContext]
  }

  class Simple(quillConfigKey: String) extends DatabaseProvider {
    override def databaseProvider: Service = new Service {
      override val database: URManaged[Blocking, DatabaseContext] =
        ZManaged
          .fromAutoCloseable(UIO(new DatabaseContext.Service(quillConfigKey) {}))
          .map(new DatabaseContext.Live(_))
    }
  }

  class WithMigration(quillConfigKey: String) extends Simple(quillConfigKey) {
    override val databaseProvider: Service = new Service {
      override val database: URManaged[Blocking, DatabaseContext] =
        WithMigration.super.databaseProvider.database.mapM { db =>
          def migrate() =
            Flyway
              .configure()
              .locations("classpath:migration")
              .dataSource(db.databaseContext.dataSource)
              .load()
              .migrate()
          zio.blocking.blocking(UIO(migrate())).map(_ => db)
        }
    }
  }
}
