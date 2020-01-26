package eventer

import java.time.{Instant, ZoneId, ZonedDateTime}

import eventer.domain.{Event, EventId}

package object infrastructure {
  final case class DbEvent(id: EventId,
                           title: String,
                           description: String,
                           host: String,
                           instant: Instant,
                           zoneId: ZoneId,
                           createdAt: Instant,
                           updatedAt: Instant) {
    val toEvent: Event = Event(
      id = id,
      title = title,
      description = description,
      host = host,
      dateTime = ZonedDateTime.ofInstant(instant, zoneId)
    )
  }
  object DbEvent {
    def fromEvent(event: Event, createdAt: Instant, updatedAt: Instant): DbEvent = DbEvent(
      id = event.id,
      title = event.title,
      description = event.description,
      host = event.host,
      instant = event.dateTime.toInstant,
      zoneId = event.dateTime.getZone,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
}
