package eventer.domain.event

import eventer.domain.{Event, EventId}
import zio.{Ref, UIO}

object InMemoryEventRepository2 {
  def empty: UIO[InMemoryEventRepository2] = Ref.make(Seq.empty[Event]).map(new InMemoryEventRepository2(_))
  def make(events: Seq[Event]): UIO[InMemoryEventRepository2] = Ref.make(events).map(new InMemoryEventRepository2(_))
}

class InMemoryEventRepository2(state: Ref[Seq[Event]]) extends EventRepository2 {
  override def create(event: Event): UIO[Unit] = state.update(_.appended(event)).unit
  override def findAll: UIO[Seq[Event]] = state.get
  override def findById(id: EventId): UIO[Option[Event]] = state.get.map(_.find(_.id == id))
}
