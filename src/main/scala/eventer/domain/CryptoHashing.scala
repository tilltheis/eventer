package eventer.domain

import zio.UIO

trait CryptoHashing[HashT] {
  def hash(unhashed: String): UIO[HashT]
  def verify(unhashed: String, hashed: HashT): UIO[Boolean]
}
