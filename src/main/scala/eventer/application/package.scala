package eventer

import eventer.domain.{EventId, UserId}
import zio.{Has, UIO, ULayer, ZLayer}

import java.util.UUID

package object application {
  type Jwts = Has[Jwts.Service]
  type EventIdGenerator = Has[UIO[EventId]]
  type UserIdGenerator = Has[UIO[UserId]]
  type AuthMiddleware = Has[AuthMiddleware.Service]
  type EventRoutes = Has[EventRoutes.Service]

  object EventIdGenerator {
    type Service = UIO[EventId]
    val live: ULayer[EventIdGenerator] = ZLayer.succeed(UIO(EventId(UUID.randomUUID())))
  }

  object UserIdGenerator {
    type Service = UIO[UserId]
    val live: ULayer[UserIdGenerator] = ZLayer.succeed(UIO(UserId(UUID.randomUUID())))
  }
}
