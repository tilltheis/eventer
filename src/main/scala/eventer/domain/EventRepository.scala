package eventer.domain

import zio.URIO

trait EventRepository[-R] {
  def create(event: Event): URIO[R, Unit]
  def findAll: URIO[R, Seq[Event]]
  def findById(id: EventId): URIO[R, Option[Event]]
}
