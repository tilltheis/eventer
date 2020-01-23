package eventer.infrastructure

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import eventer.domain.{Event, EventId, EventRepository}
import zio.{RIO, Task, ZIO}

class DbEventRepository extends EventRepository[DatabaseContext] {
  // This helper is useful because Scala doesn't allow `import db.databaseContext._` but only `import db._`.
  // This saves some boilerplate together with the code to access the `DatabaseContext`.
  protected def withCtx[A](f: DatabaseContext.Service => Task[A]): RIO[DatabaseContext, A] =
    ZIO.accessM(x => f(x.databaseContext))

  override val findAll: RIO[DatabaseContext, Seq[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event)
    performEffect(runIO(q)).map(_.map(_.toEvent))
  }

  override def findById(id: EventId): RIO[DatabaseContext, Option[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.filter(_.id == lift(id)))
    performEffect(runIO(q)).map(_.headOption.map(_.toEvent))
  }

  override def createEvent(event: Event): RIO[DatabaseContext, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(schema.event.insert(lift(DbEvent.fromEvent(event))))
    performEffect_(runIO(q))
  }

  override def createTestEvents: RIO[DatabaseContext, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(
      schema.event
        .insert(lift(DbEvent.fromEvent(Event(
          EventId(UUID.fromString("83e63830-2f40-4644-a55b-12409e660b33")),
          "title",
          "description",
          "host",
          ZonedDateTime.of(LocalDateTime.of(2019, 1, 19, 10, 3), ZoneId.of("Africa/Kinshasa")),
          LocalDateTime.of(2019, 1, 19, 10, 3),
          LocalDateTime.of(2019, 1, 19, 15, 59)
        ))))
        .onConflictIgnore)
    performEffect_(runIO(q))
  }
}
