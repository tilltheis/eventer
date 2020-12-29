package eventer.infrastructure.test

import eventer.domain.event.EventRepository
import eventer.domain.{Event, EventId}
import zio.macros.accessible
import zio._

@accessible
object InMemoryEventRepository {
  trait Service extends EventRepository.Service {
    def createAll(events: Event*): UIO[Unit] = ZIO.foreach_(events)(create)
  }

  def empty: ULayer[EventRepository with InMemoryEventRepository] = live(Seq.empty)

  def live(events: Seq[Event]): ULayer[EventRepository with InMemoryEventRepository] =
    (for {
      state <- Ref.make(events)
      repo = new InMemoryEventRepositoryServiceImpl(state)
    } yield Has.allOf(repo: EventRepository.Service, repo: InMemoryEventRepository.Service)).toLayerMany

  private class InMemoryEventRepositoryServiceImpl(state: Ref[Seq[Event]]) extends Service {
    override def create(event: Event): UIO[Unit] = state.update(_.appended(event)).unit
    override def findAll: UIO[Seq[Event]] = state.get
    override def findById(id: EventId): UIO[Option[Event]] = state.get.map(_.find(_.id == id))
  }
}
