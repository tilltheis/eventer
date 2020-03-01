package eventer.infrastructure

import eventer.domain.{Event, EventId, EventRepository}
import zio.URIO
import zio.blocking.Blocking

class DbEventRepository extends EventRepository[DatabaseContext with Blocking] {
  override def create(event: Event): URIO[DatabaseContext with Blocking, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.insert(lift(DbEvent.fromEvent(event))))
    performEffect_(runIO(q)).orDie
  }

  override val findAll: URIO[DatabaseContext with Blocking, Seq[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event)
    performEffect(runIO(q)).map(_.map(_.toEvent)).orDie
  }

  override def findById(id: EventId): URIO[DatabaseContext with Blocking, Option[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.filter(_.id == lift(id)))
    performEffect(runIO(q)).map(_.headOption.map(_.toEvent)).orDie
  }
}
