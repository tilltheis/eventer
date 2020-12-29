package eventer

import com.typesafe.scalalogging.StrictLogging
import eventer.application._
import eventer.domain._
import eventer.domain.session.SessionServiceImpl
import eventer.domain.user.UserRepository
import eventer.infrastructure.BlowfishCryptoHashing.BlowfishHash
import eventer.infrastructure.EmailSenderImpl.PasswordAuthentication
import eventer.infrastructure._
import org.http4s.server.HttpMiddleware
import zio.blocking.Blocking
import zio.clock.Clock
import zio._

import java.util.UUID

object Main extends zio.App with StrictLogging {
  val insertDemoUserIntoDb: URIO[Has[UserRepository[BlowfishHash]], Unit] =
    ZIO.service[UserRepository[BlowfishHash]].flatMap {
      _.create(
        User(
          UserId(UUID.fromString("6f31ccde-4321-4cc9-9056-6c3cbd550cba")),
          "Till",
          "example@example.org",
          BlowfishHash.unsafeFromHashString("$2a$10$d.vQEHwPIqtSYWQOMtg7LuZgTOx1R/2sOLnqCUkpixkXJ1paUhEIm")
        )).option.absorb.ignore // catch duplicate insert exceptions
    }

  val jwtsLayer: RLayer[Clock with Has[Config], Has[Jwts]] = (for {
    (clock, config) <- ZIO.services[Clock.Service, Config]
    jwtKey <- util.secretKeyFromBase64(config.server.jwtSigningKeyBase64, JwtsImpl.JwtSigningAlgorithm)
  } yield new JwtsImpl(jwtKey, clock)).toLayer

  val databaseContextLayer: URLayer[Blocking with Has[Config], DatabaseContext] =
    ZLayer.fromServiceManaged[Config, Blocking, Nothing, DatabaseContext.Service]((c: Config) =>
      DatabaseProvider.withMigration(c.db.quillConfigKey).map(_.get.database.get).build)

  val eventRoutesLayer: RLayer[DatabaseContext with Has[Jwts], Has[EventRoutes]] =
    (for {
      (dbCtx, jwts) <- ZIO.services[DatabaseContext.Service, Jwts]
      eventRepository = new DbEventRepository(dbCtx)
    } yield new EventRoutes(eventRepository, UIO(EventId(UUID.randomUUID())), Middlewares.auth(jwts))).toLayer

  val sessionRoutesLayer: RLayer[DatabaseContext with Clock with Has[Jwts] with Has[Config], Has[SessionRoutes]] =
    (for {
      (clock, config, dbCtx, jwts) <- ZIO.services[Clock.Service, Config, DatabaseContext.Service, Jwts]
      authMiddleware = Middlewares.auth(jwts)
      userRepository = new DbUserRepository[BlowfishHash](dbCtx, _.hash, BlowfishHash.unsafeFromHashString)
      sessionService = new SessionServiceImpl(userRepository, new BlowfishCryptoHashing)
    } yield new SessionRoutes(clock, jwts, sessionService, authMiddleware, config.server.useSecureCookies)).toLayer

  val userRoutesLayer: RLayer[Has[Config] with DatabaseContext, Has[UserRoutes[BlowfishHash]]] = (for {
    (config, dbCtx) <- ZIO.services[Config, DatabaseContext.Service]
    userRepository = new DbUserRepository[BlowfishHash](dbCtx, _.hash, BlowfishHash.unsafeFromHashString)
    _ <- insertDemoUserIntoDb.provideLayer(ZLayer.succeed(userRepository: UserRepository[BlowfishHash]))
    emailSender = new EmailSenderImpl(config.email.host,
                                      config.email.port,
                                      PasswordAuthentication(config.email.username, config.email.password))
  } yield
    new UserRoutes(userRepository, emailSender, new BlowfishCryptoHashing, UIO(UserId(UUID.randomUUID())))).toLayer

  val csrfMiddlewareLayer: RLayer[Has[Config], Has[HttpMiddleware[Task]]] = (for {
    config <- ZIO.service[Config]
    key <- util.secretKeyFromBase64(config.server.csrfSigningKeyBase64, Middlewares.CsrfSigningAlgorithm)
  } yield Middlewares.csrf(key, config.server.useSecureCookies)).toLayer

  val webServerLayer: URLayer[
    Has[EventRoutes] with Has[SessionRoutes] with Has[UserRoutes[BlowfishHash]] with Has[HttpMiddleware[Task]],
    Has[WebServer]] =
    ZLayer.fromServices[EventRoutes, SessionRoutes, UserRoutes[BlowfishHash], HttpMiddleware[Task], WebServer](
      (eventRoutes, sessionRoutes, userRoutes, csrfMiddleware) =>
        new WebServer(eventRoutes.routes, sessionRoutes.routes, userRoutes.routes, csrfMiddleware))

  val appLayer: RLayer[ZEnv, Has[Config] with Has[WebServer]] = {
    val routesLayers = eventRoutesLayer ++ sessionRoutesLayer ++ userRoutesLayer ++ csrfMiddlewareLayer
    ZLayer.requires[ZEnv] >+> Config.live >+> (jwtsLayer ++ databaseContextLayer) >+> routesLayers >+> webServerLayer
  }

  val runApp: RIO[Has[Config] with Has[WebServer], Unit] =
    for {
      (config, webServer) <- ZIO.services[Config, WebServer]
      result <- webServer.serve(config.server.port)
    } yield result

  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] =
    runApp
      .provideCustomLayer(appLayer)
      .mapError(logger.error("Something went wrong!", _))
      .exitCode
}
