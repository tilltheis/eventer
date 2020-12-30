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
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

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

  def layerFromConfig[R, E, A: Tag](f: Config => ZLayer[R, E, Has[A]]): ZLayer[R with Has[Config], E, Has[A]] =
    ZLayer.fromServiceManaged[Config, R, E, A](f(_).build.map(_.get))

  val jwtsLayer: RLayer[Clock with Has[Config], Jwts] = layerFromConfig(c => Jwts.live(c.server.jwtSigningKey))

  val eventRoutesLayer: RLayer[DatabaseContext with Jwts, EventRoutes] =
    DbEventRepository.live ++ EventIdGenerator.live ++ AuthMiddleware.live >>> EventRoutes.live

  val sessionRoutesLayer: RLayer[DatabaseContext with Clock with Jwts with Has[Config], Has[SessionRoutes]] =
    (for {
      (clock, config, dbCtx, jwts) <- ZIO.services[Clock.Service, Config, DatabaseContext.Service, Jwts.Service]
      authMiddleware <- AuthMiddleware.live.build.provide(Has(jwts)).useNow.map(_.get)
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

  val webServerLayer
    : URLayer[EventRoutes with Has[SessionRoutes] with Has[UserRoutes[BlowfishHash]] with Has[HttpMiddleware[Task]],
              Has[WebServer]] =
    ZLayer.fromServices[EventRoutes.Service, SessionRoutes, UserRoutes[BlowfishHash], HttpMiddleware[Task], WebServer](
      (eventRoutes, sessionRoutes, userRoutes, csrfMiddleware) =>
        new WebServer(eventRoutes.routes, sessionRoutes.routes, userRoutes.routes, csrfMiddleware))

  val appLayer: RLayer[ZEnv, Has[Config] with Has[WebServer]] = {
    val routesLayers = eventRoutesLayer ++ sessionRoutesLayer ++ userRoutesLayer ++ csrfMiddlewareLayer
    val dbLayers = ZLayer.requires[Blocking] ++ DatabaseProvider.live >>> DatabaseContext.withMigration
    val configLayers = Config.live >+> ZLayer.fromServiceMany((c: Config) => Has.allOf(c.server, c.db))
    ZLayer.requires[ZEnv] >+> configLayers >+> jwtsLayer >+> dbLayers >+> routesLayers >+> webServerLayer
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
