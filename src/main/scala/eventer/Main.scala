package eventer

import com.typesafe.scalalogging.LazyLogging
import eventer.infrastructure.{ConfigProvider, DatabaseProvider, DbEventRepository}
import zio.blocking.Blocking
import zio.{RIO, ZIO}

final case class ServerConfig(port: Int)
final case class DbConfig(url: String, username: String, password: String)
final case class Config(server: ServerConfig, db: DbConfig)

object Main extends zio.App with LazyLogging {
  val program: RIO[Blocking with DatabaseProvider with ConfigProvider, Unit] = {
    val eventRepo = new DbEventRepository()
    ZIO
      .access[DatabaseProvider](_.databaseProvider.database)
      .flatMap(_.use { dbContext =>
        ZIO.provide(dbContext) {
          for {
            _ <- eventRepo.createTestEvents
            foundEvents <- eventRepo.findEvents
          } yield logger.info(s"All events: $foundEvents")
        }
      })
  }

  lazy val env = new Blocking.Live with DatabaseProvider.Live with ConfigProvider.Live {}

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program
      .provide(env)
      .mapError(logger.error("Something went wrong!", _))
      .fold(_ => 1, _ => 0)
}
