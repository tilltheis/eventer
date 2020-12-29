package eventer.infrastructure

import eventer.DbConfig
import zio._
import zio.blocking.Blocking

object DatabaseProvider {
  trait Service {
    def database: DatabaseContext
  }

  val live: URLayer[Blocking with Has[DbConfig], DatabaseProvider] =
    ZLayer.fromServicesManaged[Blocking.Service, DbConfig, Any, Nothing, DatabaseProvider.Service](
      (blocking: Blocking.Service, dbConfig: DbConfig) =>
        ZManaged
          .fromAutoCloseable(UIO(new DatabaseContext.Service(dbConfig.quillConfigKey, blocking)))
          .map(ctx =>
            new Service {
              override def database: DatabaseContext = Has(ctx)
          }))
}
