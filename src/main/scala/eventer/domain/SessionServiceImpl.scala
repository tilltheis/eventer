package eventer.domain

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.{RIO, UIO, URIO}

class SessionServiceImpl[-R, HashT](userRepository: UserRepository[R, HashT],
                                    cryptoHashing: CryptoHashing[HashT],
                                    jwtSigningKey: String)
    extends SessionService[R] {
  override def login(loginRequest: LoginRequest): RIO[R, Option[LoginResponse]] =
    userRepository.findByEmail(loginRequest.email).flatMap { userOption =>
      (for {
        user <- RIO(userOption).someOrFailException
        _ <- cryptoHashing.verify(loginRequest.password, user.passwordHash).map(Option.when(_)(())).someOrFailException
      } yield LoginResponse(user.id, user.name, user.email)).fold(_ => None, Some.apply)
    }

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                nowEpochSeconds: Long,
                                                expiresInSeconds: Long): UIO[(String, String, String)] = {
    val claim = JwtClaim(content, issuedAt = Some(nowEpochSeconds), expiration = Some(expiresInSeconds))
    URIO(Jwt.encode(claim, jwtSigningKey, JwtAlgorithm.HS256)).map { jwtString =>
      val Array(header, payload, signature) = jwtString.split('.')
      (header, payload, signature)
    }
  }
}
