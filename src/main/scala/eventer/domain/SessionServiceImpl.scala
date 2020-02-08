package eventer.domain

import java.time.Instant

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.{RIO, UIO}

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
                                                now: Instant,
                                                expiresAt: Instant): UIO[(String, String, String)] = {
    val claim = JwtClaim(content, issuedAt = Some(now.getEpochSecond), expiration = Some(expiresAt.getEpochSecond))
    UIO(Jwt.encode(claim, jwtSigningKey, JwtAlgorithm.HS256)).map { jwtString =>
      val Array(header, payload, signature) = jwtString.split('.')
      (header, payload, signature)
    }
  }

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String,
                                                now: Instant): UIO[Option[String]] = {
    UIO(
      Jwt
        .decode(s"$header.$payload.$signature", jwtSigningKey, Seq(JwtAlgorithm.HS256), JwtOptions(expiration = false))
        .filter(_.expiration.forall(_ > now.getEpochSecond))
        .map(_.content)
        .toOption)
  }
}
