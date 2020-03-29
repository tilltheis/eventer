package eventer.domain

import java.time.Instant

import eventer.domain.InMemorySessionService.State
import eventer.domain.SessionService.{InvalidCredentials, InvalidJwtFormat}
import zio.clock.Clock
import zio._

class InMemorySessionService extends SessionService[Has[State]] {
  override def login(loginRequest: LoginRequest): ZIO[Has[State], InvalidCredentials.type, SessionUser] =
    RIO.accessM[Has[State]](_.get.stateRef.get).map(_.get(loginRequest)).someOrFail(InvalidCredentials)

  override def encodedJwtHeaderPayloadSignature(content: String,
                                                expiresAt: Instant): URIO[Clock, (String, String, String)] =
    UIO.succeed("header", eventer.base64Encode(content), "signature")

  override def decodedJwtHeaderPayloadSignature(header: String,
                                                payload: String,
                                                signature: String): ZIO[Clock, InvalidJwtFormat.type, String] =
    eventer.base64Decode(payload).orElseFail(InvalidJwtFormat)
}

object InMemorySessionService {
  final case class State(stateRef: Ref[Map[LoginRequest, SessionUser]])
  object State extends InMemoryMapStateCompanion(new State(_))
}
