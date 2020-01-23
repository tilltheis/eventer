package eventer.domain

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

object TestData {
  val eventId: EventId = EventId(UUID.fromString("83e63830-2f40-4644-a55b-12409e660b33"))

  val localDateTime: LocalDateTime = LocalDateTime.of(2019, 1, 19, 10, 3)
  val zonedDateTime: ZonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Africa/Johannesburg"))

  val event: Event = Event(
    id = eventId,
    title = "title",
    description = "description",
    host = "host",
    dateTime = zonedDateTime,
    createdAt = localDateTime,
    updatedAt = localDateTime
  )
}
