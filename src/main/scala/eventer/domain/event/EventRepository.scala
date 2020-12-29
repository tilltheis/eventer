package eventer.domain.event

import eventer.domain.{Event, EventId}
import zio.UIO
import zio.macros.accessible

@accessible
object EventRepository {
  trait Service {
    def create(event: Event): UIO[Unit]
    def findAll: UIO[Seq[Event]]
    def findById(id: EventId): UIO[Option[Event]]
  }
}
