package eventer

import java.time.{Instant, ZoneId, ZonedDateTime}

import eventer.domain.{Event, EventId, UserId}
import zio.ZIO

package object infrastructure {
  final case class DbEvent(id: EventId,
                           title: String,
                           description: String,
                           hostId: UserId,
                           instant: Instant,
                           zoneId: ZoneId) {
    val toEvent: Event = Event(
      id = id,
      title = title,
      description = description,
      hostId = hostId,
      dateTime = ZonedDateTime.ofInstant(instant, zoneId)
    )
  }
  object DbEvent {
    def fromEvent(event: Event): DbEvent = DbEvent(
      id = event.id,
      title = event.title,
      description = event.description,
      hostId = event.hostId,
      instant = event.dateTime.toInstant,
      zoneId = event.dateTime.getZone
    )
  }

  // This helper is useful because Scala doesn't allow `import db.databaseContext._` but only `import db._`.
  // This saves some boilerplate together with the code to access the `DatabaseContext`.
  private[infrastructure] def withCtx[R <: DatabaseContext, E, A](
      f: DatabaseContext.Service => ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.accessM(x => f(x.databaseContext))

}
