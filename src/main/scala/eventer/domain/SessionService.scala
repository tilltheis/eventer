package eventer.domain

import zio.{RIO, UIO}

trait SessionService[-R] {
  def login(loginRequest: LoginRequest): RIO[R, Option[LoginResponse]]
  def encodedJwtHeaderPayloadSignature(content: String,
                                       nowEpochSeconds: Long,
                                       expiresInSeconds: Long): UIO[(String, String, String)]
}
