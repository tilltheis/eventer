package eventer.domain.session

import eventer.domain.session.SessionService.InvalidCredentials
import eventer.domain.{LoginRequest, SessionUser}
import zio.IO

object SessionService {
  object InvalidCredentials
}

trait SessionService {
  def login(loginRequest: LoginRequest): IO[InvalidCredentials.type, SessionUser]
}
