package eventer.domain

import eventer.domain.BlowfishCryptoHashing.BlowfishHash
import org.mindrot.jbcrypt.BCrypt
import zio.UIO

class BlowfishCryptoHashing extends CryptoHashing[BlowfishHash] {
  override def hash(unhashed: String): UIO[BlowfishHash] =
    UIO(BlowfishHash.unsafeFromHashString(BCrypt.hashpw(unhashed, BCrypt.gensalt())))
  override def verify(unhashed: String, hashed: BlowfishHash): UIO[Boolean] = UIO(BCrypt.checkpw(unhashed, hashed.hash))
}

object BlowfishCryptoHashing {
  final case class BlowfishHash private (hash: String) extends AnyVal
  object BlowfishHash {
    private def apply(hash: String): BlowfishHash = new BlowfishHash(hash)

    def unsafeFromHashString(hash: String): BlowfishHash =
      if (hash.matches("""\$2a\$\d\d\$.{53}""")) BlowfishHash(hash)
      else throw new IllegalArgumentException(s"Hash '$hash' is not a valid Blowfish hash.")
  }
}
