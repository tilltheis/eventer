package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import zio.RIO
import zio.blocking.Blocking

class DbEventRepository extends EventRepository[DatabaseContext with Blocking] {
  override def create(event: Event): RIO[DatabaseContext with Blocking, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.insert(lift(DbEvent.fromEvent(event))))
    performEffect_(runIO(q))
  }

  override val findAll: RIO[DatabaseContext with Blocking, Seq[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event)
    performEffect(runIO(q)).map(_.map(_.toEvent))
  }

  override def findById(id: EventId): RIO[DatabaseContext with Blocking, Option[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.filter(_.id == lift(id)))
    performEffect(runIO(q)).map(_.headOption.map(_.toEvent))
  }
}
