package eventer.application

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.clock.Clock
import zio.{IO, UIO, URLayer, ZLayer}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

object Jwts {

  final object InvalidJwtFormat

  trait Service {
    def encodeJwtIntoHeaderPayloadSignature(content: String, expiresAt: Instant): UIO[(String, String, String)]

    def decodeJwtFromHeaderPayloadSignature(header: String,
                                            payload: String,
                                            signature: String): IO[InvalidJwtFormat.type, String]
  }

  val JwtSigningAlgorithm: String = JwtAlgorithm.HS256.fullName

  def live(jwtSigningKey: SecretKey): URLayer[Clock, Jwts] = ZLayer.fromService { (clock: Clock.Service) =>
    new Service {
      override def encodeJwtIntoHeaderPayloadSignature(content: String,
                                                       expiresAt: Instant): UIO[(String, String, String)] =
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
  }
}
