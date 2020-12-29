import pureconfig.error.ExceptionThrown
import pureconfig.{ConfigReader, ConfigSource}
import zio.{Has, TaskLayer, UIO}

import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import scala.util.Try

package object eventer {
  final case class ServerConfig(port: Int,
                                jwtSigningKey: SecretKey,
                                csrfSigningKeyBase64: String,
                                useSecureCookies: Boolean)
  final case class DbConfig(quillConfigKey: String)
  final case class EmailConfig(sender: String, host: String, port: Int, username: String, password: String)
  final case class Config(publicUrl: String, server: ServerConfig, db: DbConfig, email: EmailConfig)

  object Config {
    import pureconfig.generic.auto._

    implicit val secretKeyReader: ConfigReader[SecretKey] = {
      val keyAndAlgoReader = ConfigReader.fromCursor[(String, String)] { configCursor =>
        for {
          cur <- configCursor.asObjectCursor
          base64Key <- cur.atKey("base-64-key").flatMap(_.asString)
          algorithm <- cur.atKey("algorithm").flatMap(_.asString)
        } yield (base64Key, algorithm)
      }

      keyAndAlgoReader.emap {
        case (base64Key, algorithm) =>
          val result = Try(new SecretKeySpec(Base64.getDecoder.decode(base64Key.getBytes("UTF-8")), algorithm))
          result.toEither.left.map(ExceptionThrown)
      }
    }

    val live: TaskLayer[Has[Config]] = UIO(ConfigSource.default.at("eventer").loadOrThrow[Config]).absorb.toLayer
  }
}
