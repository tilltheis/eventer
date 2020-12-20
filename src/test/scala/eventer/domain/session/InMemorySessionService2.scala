package eventer.domain.session
import eventer.domain
import eventer.domain.SessionService.InvalidCredentials
import eventer.domain.{LoginRequest, SessionService, SessionUser}
import zio.{IO, Ref, UIO}

object InMemorySessionService2 {
  def empty: UIO[InMemorySessionService2] =
    Ref.make(Map.empty[LoginRequest, SessionUser]).map(new InMemorySessionService2(_))
  def make(users: Map[LoginRequest, SessionUser]): UIO[InMemorySessionService2] =
    Ref.make(users).map(new InMemorySessionService2(_))
}

class InMemorySessionService2(state: Ref[Map[LoginRequest, SessionUser]]) extends SessionService2 {
  override def login(
      loginRequest: domain.LoginRequest): IO[SessionService.InvalidCredentials.type, domain.SessionUser] =
    state.get.map(_.get(loginRequest)).someOrFail(InvalidCredentials)
}
