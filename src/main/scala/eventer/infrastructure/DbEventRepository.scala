package eventer.infrastructure

import java.time.Instant

import eventer.domain.{Event, EventId, EventRepository}
import zio.clock.Clock
import zio.{RIO, Task, ZIO}

class DbEventRepository extends EventRepository[DatabaseContext with Clock] {
  // This helper is useful because Scala doesn't allow `import db.databaseContext._` but only `import db._`.
  // This saves some boilerplate together with the code to access the `DatabaseContext`.
  protected def withCtx[R <: DatabaseContext, E, A](f: DatabaseContext.Service => ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.accessM(x => f(x.databaseContext))

  private val nowM: RIO[Clock, Instant] = ZIO.accessM[Clock](_.clock.currentDateTime).map(_.toInstant)

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

  override def create(event: Event): RIO[DatabaseContext with Clock, Unit] = withCtx { ctx =>
    import ctx._
    for {
      now <- nowM
      q = quote(schema.event.insert(lift(DbEvent.fromEvent(event, now, now))))
      res <- performEffect_(runIO(q))
    } yield res
  }
}
