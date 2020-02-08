package eventer.domain

import java.time.Instant

import eventer.domain.InMemorySessionService.State
import zio.{RIO, Ref, UIO}

class InMemorySessionService extends SessionService[State] {
  override def login(loginRequest: LoginRequest): RIO[State, Option[LoginResponse]] =
    RIO.accessM[State](_.sessionServiceStateRef.get).map(_.get(loginRequest))

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                now: Instant,
                                                expiresAt: Instant): UIO[(String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String,
                                                now: Instant): UIO[Option[String]] =
    eventer.base64Decode(payload).option
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
