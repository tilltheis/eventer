package eventer.application
import eventer.application.Jwts.InvalidJwtFormat
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.clock.Clock
import zio.{IO, UIO}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

object JwtsImpl {
  val JwtSigningAlgorithm: String = JwtAlgorithm.HS256.fullName
}

class JwtsImpl(jwtSigningKey: SecretKey, clock: Clock.Service) extends Jwts {
  override def encodeJwtIntoHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)] =
    clock.currentTime(TimeUnit.SECONDS).map { now =>
      val claim = JwtClaim(content, issuedAt = Some(now), expiration = Some(expiresAt.getEpochSecond))
      val Array(header, payload, signature) = Jwt.encode(claim, jwtSigningKey, JwtAlgorithm.HS256).split('.')
      (header, payload, signature)
    }

  override def decodeJwtFromHeaderPayloadSignature(header: String,
                                                   payload: String,
                                                   signature: String): IO[InvalidJwtFormat.type, String] = {
    clock
      .currentTime(TimeUnit.SECONDS)
      .map { now =>
        Jwt
          .decode(s"$header.$payload.$signature",
                  jwtSigningKey,
                  Seq(JwtAlgorithm.HS256),
                  JwtOptions(expiration = false))
          .filter(_.expiration.forall(_ > now))
          .map(_.content)
          .toOption
      }
      .someOrFail(InvalidJwtFormat)
  }
}
