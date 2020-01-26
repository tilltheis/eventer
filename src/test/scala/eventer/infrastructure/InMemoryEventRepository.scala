package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import eventer.infrastructure.InMemoryEventRepository.State
import zio.{RIO, Ref, UIO}

class InMemoryEventRepository extends EventRepository[State] {
  override def findAll: RIO[State, Seq[Event]] = RIO.accessM(_.stateRef.get)
  override def findById(id: EventId): RIO[State, Option[Event]] =
    RIO.accessM[State](_.stateRef.get).map(_.find(_.id == id))
  override def create(event: Event): RIO[State, Unit] =
    RIO.accessM[State](_.stateRef.update(_.appended(event))).map(_ => ())
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
