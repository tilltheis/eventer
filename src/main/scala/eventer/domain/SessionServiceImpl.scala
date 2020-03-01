package eventer.domain

import java.time.Instant
import java.util.concurrent.TimeUnit

import eventer.domain.SessionService.{InvalidCredentials, InvalidJwtFormat}
import javax.crypto.SecretKey
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.clock.Clock
import zio.{URIO, ZIO}

object SessionServiceImpl {
  val JwtSigningAlgorithm: String = JwtAlgorithm.HS256.fullName
}

class SessionServiceImpl[-R, HashT](userRepository: UserRepository[R, HashT],
                                    cryptoHashing: CryptoHashing[HashT],
                                    jwtSigningKey: SecretKey)
    extends SessionService[R] {
  override def login(loginRequest: LoginRequest): ZIO[R, InvalidCredentials.type, SessionUser] =
    for {
      user <- userRepository.findByEmail(loginRequest.email).someOrFail(InvalidCredentials)
      _ <- cryptoHashing.verify(loginRequest.password, user.passwordHash).filterOrFail(identity)(InvalidCredentials)
    } yield user.toSessionUser

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                expiresAt: Instant): URIO[Clock, (String, String, String)] =
    ZIO.accessM[Clock](_.clock.currentTime(TimeUnit.SECONDS)).map { now =>
      val claim = JwtClaim(content, issuedAt = Some(now), expiration = Some(expiresAt.getEpochSecond))
      val Array(header, payload, signature) = Jwt.encode(claim, jwtSigningKey, JwtAlgorithm.HS256).split('.')
      (header, payload, signature)
    }

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): ZIO[Clock, InvalidJwtFormat.type, String] = {
    ZIO
      .accessM[Clock](_.clock.currentTime(TimeUnit.SECONDS))
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
