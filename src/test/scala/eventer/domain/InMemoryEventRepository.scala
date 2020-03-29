package eventer.domain

import eventer.domain.InMemoryEventRepository.State
import zio.{Has, Ref, URIO}

class InMemoryEventRepository extends EventRepository[Has[State]] {
  override def create(event: Event): URIO[Has[State], Unit] =
    URIO.accessM[Has[State]](_.get.stateRef.update(_.appended(event))).unit
  override def findAll: URIO[Has[State], Seq[Event]] = URIO.accessM(_.get.stateRef.get)
  override def findById(id: EventId): URIO[Has[State], Option[Event]] =
    URIO.accessM[Has[State]](_.get.stateRef.get).map(_.find(_.id == id))
}

object InMemoryEventRepository {
  final case class State(stateRef: Ref[Seq[Event]])
  object State extends InMemorySeqStateCompanion(new State(_))
}
