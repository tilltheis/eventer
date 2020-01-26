package eventer.domain
import zio.{RIO, Ref, UIO}

class InMemoryEventRepository private (private val initialState: Seq[Event]) extends EventRepository[Any] {
  val stateM: UIO[Ref[Seq[Event]]] = Ref.make(initialState)
  override def findAll: RIO[Any, Seq[Event]] = stateM.flatMap(_.get)
  override def findById(id: EventId): RIO[Any, Option[Event]] = stateM.flatMap(_.get).map(_.find(_.id == id))
  override def create(event: Event): RIO[Any, Unit] = stateM.flatMap(_.update(_.appended(event))).map(_ => ())
}

object InMemoryEventRepository {
  def empty: InMemoryEventRepository = new InMemoryEventRepository(Seq.empty)
  def make(initialState: Seq[Event]): InMemoryEventRepository = new InMemoryEventRepository(initialState)
}
