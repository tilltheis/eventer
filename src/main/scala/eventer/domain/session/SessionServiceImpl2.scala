package eventer.domain.session

import eventer.domain.SessionService.InvalidCredentials
import eventer.domain.user.UserRepository2
import eventer.domain.{CryptoHashing, LoginRequest, SessionUser}
import zio.IO

class SessionServiceImpl2[HashT](userRepository: UserRepository2[HashT], cryptoHashing: CryptoHashing[HashT])
    extends SessionService2 {
  override def login(loginRequest: LoginRequest): IO[InvalidCredentials.type, SessionUser] =
    for {
      user <- userRepository.findByEmail(loginRequest.email).someOrFail(InvalidCredentials)
      _ <- cryptoHashing.verify(loginRequest.password, user.passwordHash).filterOrFail(identity)(InvalidCredentials)
    } yield user.toSessionUser
}
