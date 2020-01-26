package eventer.domain

import zio.RIO

trait EventRepository[R] {
  def findAll: RIO[R, Seq[Event]]
  def findById(id: EventId): RIO[R, Option[Event]]
  def create(event: Event): RIO[R, Unit]
}
