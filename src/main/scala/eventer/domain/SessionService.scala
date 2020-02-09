package eventer.domain

import java.time.Instant

import zio.clock.Clock
import zio.{RIO, URIO}

trait SessionService[-R] {
  def login(loginRequest: LoginRequest): RIO[R, Option[SessionUser]]
  def encodedJwtHeaderPayloadSignature(content: String, expiresAt: Instant): URIO[Clock, (String, String, String)]
  def decodedJwtHeaderPayloadSignature(header: String, payload: String, signature: String): URIO[Clock, Option[String]]
}
