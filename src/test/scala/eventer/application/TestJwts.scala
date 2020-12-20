package eventer.application
import eventer.domain.SessionService
import zio.{IO, UIO}

import java.time.Instant

class TestJwts extends Jwts {
  override def encodedJwtHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): IO[SessionService.InvalidJwtFormat.type, String] =
    ???
}
