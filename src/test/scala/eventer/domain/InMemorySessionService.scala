package eventer.domain

import java.time.Instant

import eventer.domain.InMemorySessionService.State
import eventer.domain.SessionService.{InvalidCredentials, InvalidJwtFormat}
import zio.clock.Clock
import zio._

class InMemorySessionService extends SessionService[Has[State]] {
  override def login(loginRequest: LoginRequest): ZIO[Has[State], InvalidCredentials.type, SessionUser] =
    RIO.accessM[Has[State]](_.get.sessionServiceStateRef.get).map(_.get(loginRequest)).someOrFail(InvalidCredentials)

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                expiresAt: Instant): URIO[Clock, (String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): ZIO[Clock, InvalidJwtFormat.type, String] =
    eventer.base64Decode(payload).mapError(_ => InvalidJwtFormat)
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
