package eventer.domain

import java.time.Instant

import eventer.domain.InMemorySessionService.State
import zio.clock.Clock
import zio.{RIO, Ref, UIO, URIO}

class InMemorySessionService extends SessionService[State] {
  override def login(loginRequest: LoginRequest): RIO[State, Option[SessionUser]] =
    RIO.accessM[State](_.sessionServiceStateRef.get).map(_.get(loginRequest))

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                expiresAt: Instant): URIO[Clock, (String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): URIO[Clock, Option[String]] =
    eventer.base64Decode(payload).option
}

object InMemorySessionService {
  trait State {
    def sessionServiceStateRef: Ref[Map[LoginRequest, SessionUser]]
  }

  def makeState(state: Map[LoginRequest, SessionUser]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val sessionServiceStateRef: Ref[Map[LoginRequest, SessionUser]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Map.empty)
}
