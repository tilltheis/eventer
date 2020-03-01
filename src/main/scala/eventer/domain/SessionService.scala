package eventer.domain

import java.time.Instant

import eventer.domain.SessionService.{InvalidCredentials, InvalidJwtFormat}
import zio.clock.Clock
import zio.{URIO, ZIO}

trait SessionService[-R] {
  def login(loginRequest: LoginRequest): ZIO[R, InvalidCredentials.type, SessionUser]
  def encodedJwtHeaderPayloadSignature(content: String, expiresAt: Instant): URIO[Clock, (String, String, String)]
  def decodedJwtHeaderPayloadSignature(header: String,
                                       payload: String,
                                       signature: String): ZIO[Clock, InvalidJwtFormat.type, String]
}

object SessionService {
  object InvalidCredentials
  object InvalidJwtFormat
}
