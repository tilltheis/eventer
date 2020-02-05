package eventer.domain
import zio.UIO

class PlaintextCryptoHashing extends CryptoHashing[String] {
  override def hash(unhashed: String): UIO[String] = UIO.succeed(unhashed)
  override def verify(unhashed: String, hashed: String): UIO[Boolean] = UIO.succeed(unhashed == hashed)
}
