package eventer.domain
import zio.RIO

class TestEventRepository[R] extends EventRepository[R] {
  override def findAll: RIO[R, Seq[Event]] = ???
  override def findById(id: EventId): RIO[R, Option[Event]] = ???
  override def createEvent(event: Event): RIO[R, Unit] = ???
  override def createTestEvents: RIO[R, Unit] = ???
}
