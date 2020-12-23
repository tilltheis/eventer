package eventer.domain.session

import eventer.domain
import eventer.domain.session.SessionService.InvalidCredentials
import eventer.domain.{LoginRequest, SessionUser}
import zio.{IO, Ref, UIO}

object InMemorySessionService {
  def empty: UIO[InMemorySessionService] =
    Ref.make(Map.empty[LoginRequest, SessionUser]).map(new InMemorySessionService(_))
  def make(users: Map[LoginRequest, SessionUser]): UIO[InMemorySessionService] =
    Ref.make(users).map(new InMemorySessionService(_))
}

class InMemorySessionService(state: Ref[Map[LoginRequest, SessionUser]]) extends SessionService {
  override def login(loginRequest: domain.LoginRequest): IO[InvalidCredentials.type, domain.SessionUser] =
    state.get.map(_.get(loginRequest)).someOrFail(InvalidCredentials)
}
