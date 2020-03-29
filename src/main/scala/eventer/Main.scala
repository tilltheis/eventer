package eventer

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import eventer.application.WebServer
import eventer.domain.BlowfishCryptoHashing.BlowfishHash
import eventer.domain._
import eventer.infrastructure._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{UIO, ZIO, ZLayer}

final case class ServerConfig(port: Int, jwtSigningKeyBase64: String, csrfSigningKeyBase64: String)
final case class DbConfig(url: String, username: String, password: String, quillConfigKey: String)
final case class Config(server: ServerConfig, db: DbConfig)

object Main extends zio.App with LazyLogging {
  type MainEnvironment = Clock with Blocking
  type ApplicationEnvironment = Clock with Blocking with DatabaseContext

  def applicationLayer(config: Config): ZLayer[MainEnvironment, Nothing, ApplicationEnvironment] = {
    val db = DatabaseProvider.withMigration(config.db.quillConfigKey)
    val ctx = ZLayer.fromFunctionMany[DatabaseProvider, DatabaseContext](_.get.database)
    (db >>> ctx) ++ ZLayer.requires[Blocking] ++ ZLayer.requires[Clock]
  }

  def application(config: Config): ZIO[ApplicationEnvironment, Throwable, Unit] = {
    (ZIO
      .accessM[ApplicationEnvironment] { appEnv =>
        val userRepository = new DbUserRepository[BlowfishHash](_.hash, BlowfishHash.unsafeFromHashString)
        val eventRepository = new DbEventRepository()
        val cryptoHashing = new BlowfishCryptoHashing()

        for {
          // example user with email "example@example.org" and password "password"
          _ <- userRepository
            .create(User(
              UserId(UUID.fromString("6f31ccde-4321-4cc9-9056-6c3cbd550cba")),
              "Till",
              "example@example.org",
              BlowfishHash.unsafeFromHashString("$2a$10$d.vQEHwPIqtSYWQOMtg7LuZgTOx1R/2sOLnqCUkpixkXJ1paUhEIm")
            ))
            .option
            .absorb
            .catchAll(_ => ZIO.unit) // catch duplicate insert exceptions
            .provide(appEnv)
          jwtKey <- util.secretKeyFromBase64(config.server.jwtSigningKeyBase64, SessionServiceImpl.JwtSigningAlgorithm)
          csrfKey <- util.secretKeyFromBase64(config.server.csrfSigningKeyBase64, WebServer.CsrfSigningAlgorithm)
          sessionService = new SessionServiceImpl(userRepository, cryptoHashing, jwtKey)
          webServer = new WebServer(eventRepository,
                                    sessionService,
                                    userRepository,
                                    cryptoHashing,
                                    UIO(EventId(UUID.randomUUID())),
                                    UIO(UserId(UUID.randomUUID())),
                                    csrfKey)
          serving <- webServer.serve(config.server.port).provide(appEnv)
        } yield serving
      })
      .unit
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    for {
      config <- ConfigProvider.Live.configProvider.config
      result <- application(config)
        .provideLayer(applicationLayer(config))
        .mapError(logger.error("Something went wrong!", _))
        .fold(_ => 1, _ => 0)
    } yield result
}
