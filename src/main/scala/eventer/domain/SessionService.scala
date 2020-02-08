package eventer.domain

import java.time.Instant

import zio.{RIO, UIO}

trait SessionService[-R] {
  def login(loginRequest: LoginRequest): RIO[R, Option[LoginResponse]]
  def encodedJwtHeaderPayloadSignature(content: String, now: Instant, expiresAt: Instant): UIO[(String, String, String)]
  def decodedJwtHeaderPayloadSignature(header: String,
                                       payload: String,
                                       signature: String,
                                       now: Instant): UIO[Option[String]]
}
