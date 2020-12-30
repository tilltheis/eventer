package eventer.domain.session

import eventer.domain.session.SessionService.InvalidCredentials
import eventer.domain.user.UserRepository
import eventer.domain.{CryptoHashing, LoginRequest, SessionUser}
import zio.IO

class SessionServiceImpl[HashT](userRepository: UserRepository.Service[HashT], cryptoHashing: CryptoHashing[HashT])
    extends SessionService {
  override def login(loginRequest: LoginRequest): IO[InvalidCredentials.type, SessionUser] =
    for {
      user <- userRepository.findByEmail(loginRequest.email).someOrFail(InvalidCredentials)
      _ <- cryptoHashing.verify(loginRequest.password, user.passwordHash).filterOrFail(identity)(InvalidCredentials)
    } yield user.toSessionUser
}
