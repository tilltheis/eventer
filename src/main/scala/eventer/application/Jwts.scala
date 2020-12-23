package eventer.application

import eventer.application.Jwts.InvalidJwtFormat
import zio.{IO, UIO}

import java.time.Instant

object Jwts {
  object InvalidJwtFormat
}

trait Jwts {
  def encodeJwtIntoHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)]
  def decodeJwtFromHeaderPayloadSignature(header: String,
                                          payload: String,
                                          signature: String): IO[InvalidJwtFormat.type, String]
}
