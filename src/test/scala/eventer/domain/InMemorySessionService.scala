package eventer.domain

import eventer.domain.InMemorySessionService.State
import zio.{RIO, Ref, UIO}

class InMemorySessionService extends SessionService[State] {
  override def login(loginRequest: LoginRequest): RIO[State, Option[LoginResponse]] =
    RIO.accessM[State](_.sessionServiceStateRef.get).map(_.get(loginRequest))

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                nowEpochSeconds: Long,
                                                expiresInSeconds: Long): UIO[(String, String, String)] =
    UIO.succeed("header", content, "signature")
}

object InMemorySessionService {
  trait State {
    def sessionServiceStateRef: Ref[Map[LoginRequest, LoginResponse]]
  }

  def makeState(state: Map[LoginRequest, LoginResponse]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val sessionServiceStateRef: Ref[Map[LoginRequest, LoginResponse]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Map.empty)
}
