package eventer.infrastructure

import eventer.domain.CryptoHashing
import eventer.infrastructure.BlowfishCryptoHashing.BlowfishHash
import org.mindrot.jbcrypt.BCrypt
import zio.{Has, UIO, ULayer, ZLayer}

object BlowfishCryptoHashing {
  final case class BlowfishHash private (hash: String) extends AnyVal {
    private def copy(): Unit = ()
  }

  object BlowfishHash {
    private def apply(hash: String): BlowfishHash = new BlowfishHash(hash)

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFromHashString(hash: String): BlowfishHash =
      if (hash.matches("""\$2a\$\d\d\$.{53}""")) BlowfishHash(hash)
      else throw new IllegalArgumentException(s"Hash '$hash' is not a valid Blowfish hash.")
  }

  val live: ULayer[Has[CryptoHashing[BlowfishHash]]] = ZLayer.succeed(new BlowfishCryptoHashing)
}

class BlowfishCryptoHashing extends CryptoHashing[BlowfishHash] {
  override def hash(unhashed: String): UIO[BlowfishHash] =
    UIO(BlowfishHash.unsafeFromHashString(BCrypt.hashpw(unhashed, BCrypt.gensalt())))
  override def verify(unhashed: String, hashed: BlowfishHash): UIO[Boolean] = UIO(BCrypt.checkpw(unhashed, hashed.hash))
}
