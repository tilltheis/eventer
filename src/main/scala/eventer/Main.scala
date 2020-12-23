package eventer

import java.util.UUID
import com.typesafe.scalalogging.StrictLogging
import eventer.application.{EventRoutes, JwtsImpl, Middlewares, SessionRoutes, UserRoutes, WebServer}
import eventer.domain._
import eventer.domain.session.SessionServiceImpl
import eventer.infrastructure.BlowfishCryptoHashing.BlowfishHash
import eventer.infrastructure.EmailSenderImpl.PasswordAuthentication
import eventer.infrastructure._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{UIO, URIO, URLayer, ZIO, ZLayer}

final case class ServerConfig(port: Int,
                              jwtSigningKeyBase64: String,
                              csrfSigningKeyBase64: String,
                              useSecureCookies: Boolean)
final case class DbConfig(url: String, username: String, password: String, quillConfigKey: String)
final case class EmailConfig(sender: String, host: String, port: Int, username: String, password: String)
final case class Config(publicUrl: String, server: ServerConfig, db: DbConfig, email: EmailConfig)

object Main extends zio.App with StrictLogging {
  type MainEnvironment = Clock with Blocking
  type ApplicationEnvironment = Clock with Blocking with DatabaseContext

  def applicationLayer(config: Config): URLayer[MainEnvironment, ApplicationEnvironment] = {
    val ctx = DatabaseProvider.withMigration(config.db.quillConfigKey).map(_.get.database)
    ctx ++ ZLayer.requires[Blocking] ++ ZLayer.requires[Clock]
  }

  def application(config: Config): ZIO[ApplicationEnvironment, Throwable, Unit] = {
    ZIO.service[DatabaseContext.Service].flatMap { dbCtx =>
      ZIO.service[Clock.Service].flatMap { clock =>
        val userRepository = new DbUserRepository[BlowfishHash](dbCtx, _.hash, BlowfishHash.unsafeFromHashString)
        val eventRepository = new DbEventRepository(dbCtx)
        val emailSender =
          new EmailSenderImpl(config.email.host,
                              config.email.port,
                              PasswordAuthentication(config.email.username, config.email.password))
        val cryptoHashing = new BlowfishCryptoHashing()

        (for {
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
            .ignore // catch duplicate insert exceptions
          jwtKey <- util.secretKeyFromBase64(config.server.jwtSigningKeyBase64, JwtsImpl.JwtSigningAlgorithm)
          csrfKey <- util.secretKeyFromBase64(config.server.csrfSigningKeyBase64, Middlewares.CsrfSigningAlgorithm)
          sessionService = new SessionServiceImpl(userRepository, cryptoHashing)
          jwts = new JwtsImpl(jwtKey, clock)
          webServer = new WebServer(
            eventRoutes =
              new EventRoutes(eventRepository, UIO(EventId(UUID.randomUUID())), Middlewares.auth(jwts)).routes,
            sessionRoutes = new SessionRoutes(clock,
                                              jwts,
                                              sessionService,
                                              Middlewares.auth(jwts),
                                              config.server.useSecureCookies).routes,
            userRoutes =
              new UserRoutes(userRepository, emailSender, cryptoHashing, UIO(UserId(UUID.randomUUID()))).routes,
            Middlewares.csrf(csrfKey, config.server.useSecureCookies)
          )
          serving <- webServer.serve(config.server.port)
        } yield serving).unit
      }
    }
  }

  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] =
    for {
      config <- UIO(ConfigSource.default.at("eventer").loadOrThrow[Config])
      result <- application(config)
        .provideLayer(applicationLayer(config))
        .mapError(logger.error("Something went wrong!", _))
        .exitCode
    } yield result
}
