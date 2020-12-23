package eventer.infrastructure

import eventer.domain.event.EventRepository
import eventer.domain.{Event, EventId}
import zio.UIO

class DbEventRepository(ctx: DatabaseContext.Service) extends EventRepository {
  import ctx._

  override def create(event: Event): UIO[Unit] = {
    val q = quote(schema.event.insert(lift(DbEvent.fromEvent(event))))
    performEffect_2(runIO(q)).orDie
  }

  override val findAll: UIO[Seq[Event]] = {
    val q = quote(schema.event)
    performEffect2(runIO(q)).map(_.map(_.toEvent)).orDie
  }

  override def findById(id: EventId): UIO[Option[Event]] = {
    val q = quote(schema.event.filter(_.id == lift(id)))
    performEffect2(runIO(q)).map(_.headOption.map(_.toEvent)).orDie
  }
}
