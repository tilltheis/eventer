import pureconfig.ConfigSource
import zio.{Has, TaskLayer, UIO}

package object eventer {
  final case class ServerConfig(port: Int,
                                jwtSigningKeyBase64: String,
                                csrfSigningKeyBase64: String,
                                useSecureCookies: Boolean)
  final case class DbConfig(url: String, username: String, password: String, quillConfigKey: String)
  final case class EmailConfig(sender: String, host: String, port: Int, username: String, password: String)
  final case class Config(publicUrl: String, server: ServerConfig, db: DbConfig, email: EmailConfig)

  object Config {
    import pureconfig.generic.auto._
    val live: TaskLayer[Has[Config]] = UIO(ConfigSource.default.at("eventer").loadOrThrow[Config]).absorb.toLayer
  }
}
