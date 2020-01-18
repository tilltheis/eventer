package eventer

import java.time.LocalDateTime

import com.typesafe.scalalogging.LazyLogging
import io.getquill.{PostgresJdbcContext, SnakeCase}
import org.flywaydb.core.Flyway
import pureconfig.ConfigSource

final case class ServerConfig(port: Int)
final case class DbConfig(url: String, username: String, password: String)
final case class Config(server: ServerConfig, db: DbConfig)

object Main extends App with LazyLogging {
  logger.info("hello world")

  val config = {
    import pureconfig.generic.auto._
    ConfigSource.default.at("eventer").loadOrThrow[Config]
  }

  val ctx = new PostgresJdbcContext(SnakeCase, "quill")
  try {
    val flyway = Flyway
      .configure()
      .locations("classpath:migration")
      .dataSource(ctx.dataSource)
      .load()
    flyway.migrate()

    import ctx._
    val q = quote(infix"""SELECT now()""".as[Query[LocalDateTime]])
    val now = performIO(ctx.runIO(q)).head
    logger.info(s"What time is it? It's $now time!")
  } finally ctx.dataSource.close()
}
