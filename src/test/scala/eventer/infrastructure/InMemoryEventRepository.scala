package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import eventer.infrastructure.InMemoryEventRepository.State
import zio.{Ref, UIO, URIO, ZIO}

class InMemoryEventRepository extends EventRepository[State] {
  override def create(event: Event): URIO[State, Unit] =
    URIO.accessM[State](_.eventRepositoryStateRef.update(_.appended(event))).unit
  override def findAll: URIO[State, Seq[Event]] = URIO.accessM(_.eventRepositoryStateRef.get)
  override def findById(id: EventId): URIO[State, Option[Event]] =
    URIO.accessM[State](_.eventRepositoryStateRef.get).map(_.find(_.id == id))
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
