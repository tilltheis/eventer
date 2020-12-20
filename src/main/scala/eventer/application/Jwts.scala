package eventer.application

import eventer.domain.SessionService.InvalidJwtFormat
import zio.{IO, UIO}

import java.time.Instant

trait Jwts {
  def encodedJwtHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)]
  def decodedJwtHeaderPayloadSignature(header: String,
                                       payload: String,
                                       signature: String): IO[InvalidJwtFormat.type, String]
}
