package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import eventer.infrastructure.InMemoryEventRepository.State
import zio.{RIO, Ref, UIO}

class InMemoryEventRepository extends EventRepository[State] {
  override def findAll: RIO[State, Seq[Event]] = RIO.accessM(_.eventRepositoryStateRef.get)
  override def findById(id: EventId): RIO[State, Option[Event]] =
    RIO.accessM[State](_.eventRepositoryStateRef.get).map(_.find(_.id == id))
  override def create(event: Event): RIO[State, Unit] =
    RIO.accessM[State](_.eventRepositoryStateRef.update(_.appended(event))).unit
}

object InMemoryEventRepository {
  trait State {
    def eventRepositoryStateRef: Ref[Seq[Event]]
  }

  def makeState(state: Seq[Event]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val eventRepositoryStateRef: Ref[Seq[Event]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Seq.empty)
}
