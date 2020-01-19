package eventer

import com.typesafe.scalalogging.LazyLogging
import eventer.application.WebServer
import eventer.infrastructure.{ConfigProvider, DatabaseContext, DatabaseProvider, DbEventRepository}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{RIO, ZIO}

final case class ServerConfig(port: Int)
final case class DbConfig(url: String, username: String, password: String)
final case class Config(server: ServerConfig, db: DbConfig)

object Main extends zio.App with LazyLogging {

  type AppEnvironment = Clock with Blocking with DatabaseProvider with ConfigProvider
  lazy val appEnvironment: AppEnvironment = new Clock.Live with Blocking.Live with DatabaseProvider.Live
  with ConfigProvider.Live {}

  val program: RIO[AppEnvironment, Unit] = for {
    db <- ZIO.access[DatabaseProvider](_.databaseProvider.database)
    eventRepo = new DbEventRepository()
    webServer = new WebServer(eventRepo)
    envClock <- ZIO.environment[Clock]
    envBlocking <- ZIO.environment[Blocking]
    _ <- webServer.serve.provideManaged(
      db.map(ctx =>
          new Clock with DatabaseContext {
            override val clock: Clock.Service[Any] = envClock.clock
            override def databaseContext: DatabaseContext.Service = ctx
        })
        .provide(envBlocking))
  } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program
      .provide(appEnvironment)
      .mapError(logger.error("Something went wrong!", _))
      .fold(_ => 1, _ => 0)
}
