package eventer

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import eventer.application.WebServer
import eventer.domain.BlowfishCryptoHashing.BlowfishHash
import eventer.domain.{BlowfishCryptoHashing, SessionService, SessionServiceImpl, User, UserId}
import eventer.infrastructure.{ConfigProvider, DatabaseContext, DatabaseProvider, DbEventRepository, DbUserRepository}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{RIO, RManaged, UIO, ZIO, ZManaged}

final case class ServerConfig(port: Int, jwtSigningKey: String)
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

  type ApplicationEnvironment = Clock with Blocking with DatabaseContext with ConfigProvider
  val applicationEnvironment: RManaged[MainEnvironment, ApplicationEnvironment] = for {
    mainEnv <- ZManaged.environment[MainEnvironment]
    dbContext <- mainEnv.databaseProvider.database
  } yield
    new Clock with Blocking with DatabaseContext with ConfigProvider {
      override val clock: Clock.Service[Any] = mainEnv.clock
      override val blocking: Blocking.Service[Any] = mainEnv.blocking
      override def databaseContext: DatabaseContext.Service = dbContext.databaseContext
      override def configProvider: ConfigProvider.Service = mainEnv.configProvider
    }

  val program: ZIO[MainEnvironment, Throwable, Unit] = {
    applicationEnvironment.use { appEnv =>
      appEnv.configProvider.config.flatMap { config =>
        val userRepository = new DbUserRepository[BlowfishHash](_.hash, BlowfishHash.unsafeFromHashString)
        val eventRepository = new DbEventRepository()
        val cryptoHashing = new BlowfishCryptoHashing()
        val sessionService = new SessionServiceImpl[ApplicationEnvironment, BlowfishHash](userRepository,
                                                                                          cryptoHashing,
                                                                                          config.server.jwtSigningKey)
        val webServer = new WebServer(eventRepository, sessionService, UIO(UUID.randomUUID().toString))

        for {
          _ <- userRepository
            .create(User(
              UserId(UUID.fromString("6f31ccde-4321-4cc9-9056-6c3cbd550cba")),
              "Till",
              "example@example.org",
              BlowfishHash.unsafeFromHashString("$2a$10$d.vQEHwPIqtSYWQOMtg7LuZgTOx1R/2sOLnqCUkpixkXJ1paUhEIm")
            ))
            .catchAll(_ => ZIO.unit) // catch duplicate insert exceptions
            .provide(appEnv)
          serving <- webServer.serve(config.server.port).provide(appEnv)
        } yield serving
      }
    }
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program
      .provide(mainEnvironment)
      .mapError(logger.error("Something went wrong!", _))
      .fold(_ => 1, _ => 0)
}
