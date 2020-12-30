package eventer

import eventer.domain.EventId
import zio.{Has, UIO, ULayer, ZLayer}

import java.util.UUID

package object application {
  type Jwts = Has[Jwts.Service]
  type EventIdGenerator = Has[UIO[EventId]]
  type AuthMiddleware = Has[AuthMiddleware.Service]
  type EventRoutes = Has[EventRoutes.Service]

  object EventIdGenerator {
    type Service = UIO[EventId]
    val live: ULayer[EventIdGenerator] = ZLayer.succeed(UIO(EventId(UUID.randomUUID())))
  }
}
