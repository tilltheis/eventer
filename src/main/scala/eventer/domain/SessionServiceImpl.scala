package eventer.domain

import java.time.Instant
import java.util.concurrent.TimeUnit

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.clock.Clock
import zio.{RIO, URIO, ZIO}

class SessionServiceImpl[-R, HashT](userRepository: UserRepository[R, HashT],
                                    cryptoHashing: CryptoHashing[HashT],
                                    jwtSigningKey: String)
    extends SessionService[R] {
  override def login(loginRequest: LoginRequest): RIO[R, Option[LoginResponse]] =
    userRepository.findByEmail(loginRequest.email).flatMap { userOption =>
      (for {
        user <- RIO(userOption).someOrFailException
        _ <- cryptoHashing.verify(loginRequest.password, user.passwordHash).filterOrFail(identity)(new RuntimeException)
      } yield LoginResponse(user.id, user.name, user.email)).option
    }

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                expiresAt: Instant): URIO[Clock, (String, String, String)] =
    ZIO.accessM[Clock](_.clock.currentTime(TimeUnit.SECONDS)).map { now =>
      val claim = JwtClaim(content, issuedAt = Some(now), expiration = Some(expiresAt.getEpochSecond))
      val Array(header, payload, signature) = Jwt.encode(claim, jwtSigningKey, JwtAlgorithm.HS256).split('.')
      (header, payload, signature)
    }

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): URIO[Clock, Option[String]] = {
    ZIO.accessM[Clock](_.clock.currentTime(TimeUnit.SECONDS)).map { now =>
      Jwt
        .decode(s"$header.$payload.$signature", jwtSigningKey, Seq(JwtAlgorithm.HS256), JwtOptions(expiration = false))
        .filter(_.expiration.forall(_ > now))
        .map(_.content)
        .toOption
    }
  }
}
