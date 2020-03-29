package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import eventer.infrastructure.InMemoryEventRepository.State
import zio.{Has, Ref, UIO, URIO}

class InMemoryEventRepository extends EventRepository[Has[State]] {
  override def create(event: Event): URIO[Has[State], Unit] =
    URIO.accessM[Has[State]](_.get.stateRef.update(_.appended(event))).unit
  override def findAll: URIO[Has[State], Seq[Event]] = URIO.accessM(_.get.stateRef.get)
  override def findById(id: EventId): URIO[Has[State], Option[Event]] =
    URIO.accessM[Has[State]](_.get.stateRef.get).map(_.find(_.id == id))
}

object InMemoryEventRepository {
  trait State {
    def stateRef: Ref[Seq[Event]]
  }

  def makeState(state: Seq[Event]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val stateRef: Ref[Seq[Event]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Seq.empty)
}
