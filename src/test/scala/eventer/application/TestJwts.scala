package eventer.application

import eventer.Base64
import eventer.application.Jwts.InvalidJwtFormat
import zio.{IO, UIO}

import java.time.Instant

object TestJwts extends Jwts.Service {
  override def encodeJwtIntoHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)] =
    UIO.succeed("header", Base64.encode(content), "signature")

  override def decodeJwtFromHeaderPayloadSignature(header: String,
                                                   payload: String,
                                                   signature: String): IO[InvalidJwtFormat.type, String] =
    Base64.decode(payload).orElseFail(InvalidJwtFormat)
}
