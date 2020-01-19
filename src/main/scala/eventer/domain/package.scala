package eventer

import java.time.LocalDateTime
import java.util.UUID

package object domain {
  final case class EventId(id: UUID) extends AnyVal
  final case class Event(id: EventId,
                         title: String,
                         description: String,
                         host: String,
                         createdAt: LocalDateTime,
                         updatedAt: LocalDateTime)
}
