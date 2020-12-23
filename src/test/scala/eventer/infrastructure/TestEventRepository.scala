package eventer.infrastructure

import eventer.domain.event.EventRepository
import eventer.domain.{Event, EventId}
import zio.{Ref, UIO}

object TestEventRepository {
  def empty: UIO[TestEventRepository] = Ref.make(Seq.empty[Event]).map(new TestEventRepository(_))
  def make(events: Seq[Event]): UIO[TestEventRepository] = Ref.make(events).map(new TestEventRepository(_))
}

class TestEventRepository(state: Ref[Seq[Event]]) extends EventRepository {
  override def create(event: Event): UIO[Unit] = state.update(_.appended(event)).unit
  override def findAll: UIO[Seq[Event]] = state.get
  override def findById(id: EventId): UIO[Option[Event]] = state.get.map(_.find(_.id == id))
}
