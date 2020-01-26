package eventer

import com.typesafe.scalalogging.LazyLogging
import eventer.application.WebServer
import eventer.infrastructure.{ConfigProvider, DatabaseContext, DatabaseProvider, DbEventRepository}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{RManaged, ZIO, ZManaged}

final case class ServerConfig(port: Int)
final case class DbConfig(url: String, username: String, password: String, quillConfigKey: String)
final case class Config(server: ServerConfig, db: DbConfig)

object Main extends zio.App with LazyLogging {
  type MainEnvironment = Clock with Blocking with DatabaseProvider with ConfigProvider
  lazy val mainEnvironment: MainEnvironment = new Clock.Live with Blocking.Live with DatabaseProvider
  with ConfigProvider.Live {
    override def databaseProvider: DatabaseProvider.Service = new DatabaseProvider.Service {
      override def database: RManaged[Blocking, DatabaseContext] =
        for {
          config <- ZManaged.fromEffect(configProvider.config)
          databaseContext <- new DatabaseProvider.WithMigration(config.db.quillConfigKey).databaseProvider.database
        } yield databaseContext
    }
  }

  type ApplicationEnvironment = Clock with Blocking with DatabaseContext
  val applicationEnvironment: RManaged[MainEnvironment, ApplicationEnvironment] = for {
    mainEnv <- ZManaged.environment[MainEnvironment]
    dbContext <- mainEnv.databaseProvider.database
  } yield
    new Clock with Blocking with DatabaseContext {
      override val clock: Clock.Service[Any] = mainEnv.clock
      override val blocking: Blocking.Service[Any] = mainEnv.blocking
      override def databaseContext: DatabaseContext.Service = dbContext.databaseContext
    }

  val program: ZIO[MainEnvironment, Throwable, Unit] = {
    val eventRepo = new DbEventRepository()
    val webServer = new WebServer(eventRepo)
    applicationEnvironment.use(webServer.serve.provide)
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program
      .provide(mainEnvironment)
      .mapError(logger.error("Something went wrong!", _))
      .fold(_ => 1, _ => 0)
}
