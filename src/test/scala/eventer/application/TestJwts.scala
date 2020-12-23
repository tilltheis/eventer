package eventer.application

import eventer.application.Jwts.InvalidJwtFormat
import zio.{IO, UIO}

import java.time.Instant

object TestJwts extends Jwts {
  override def encodeJwtIntoHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodeJwtFromHeaderPayloadSignature(header: String,
                                                   payload: String,
                                                   signature: String): IO[InvalidJwtFormat.type, String] =
    eventer.base64Decode(payload).orElseFail(InvalidJwtFormat)
}