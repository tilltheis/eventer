package eventer.infrastructure

import java.time.LocalDateTime
import java.util.UUID

import eventer.domain.{Event, EventId, EventRepository}
import zio.{RIO, Task, ZIO}

class DbEventRepository extends EventRepository[DatabaseContext] {
  // This helper is useful because Scala doesn't allow `import db.databaseContext._` but only `import db._`.
  // This saves some boilerplate together with the code to access the `DatabaseContext`.
  protected def withCtx[A](f: DatabaseContext => Task[A]): RIO[DatabaseContext, A] =
    ZIO.accessM(f)

  override val findEvents: RIO[DatabaseContext, Seq[Event]] = withCtx { ctx =>
    import ctx._
    val q = quote(query[Event])
    performEffect(runIO(q))
  }

  override def createTestEvents: RIO[DatabaseContext, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(
      query[Event]
        .insert(lift(Event(
          EventId(UUID.fromString("83e63830-2f40-4644-a55b-12409e660b33")),
          "title",
          "description",
          "host",
          LocalDateTime.of(2019, 1, 19, 10, 3),
          LocalDateTime.of(2019, 1, 19, 15, 59)
        )))
        .onConflictIgnore)
    performEffect_(runIO(q))
  }
}
