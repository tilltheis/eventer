package eventer

import java.util.Base64

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import zio.Task

object util {
  def unsafeSecretKeyFromBase64(base64String: String, algorithm: String): SecretKey =
    new SecretKeySpec(Base64.getDecoder.decode(base64String), algorithm)

  def secretKeyFromBase64(base64String: String, algorithm: String): Task[SecretKey] =
    Task(unsafeSecretKeyFromBase64(base64String, algorithm))
}
