package eventer.domain.session

import eventer.domain.SessionService.InvalidCredentials
import eventer.domain.{LoginRequest, SessionUser}
import zio.IO

trait SessionService2 {
  def login(loginRequest: LoginRequest): IO[InvalidCredentials.type, SessionUser]
}
