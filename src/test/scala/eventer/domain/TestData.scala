package eventer.domain

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

object TestData {
  val eventId: EventId = EventId(UUID.fromString("83e63830-2f40-4644-a55b-12409e660b33"))
  val userId: UserId = UserId(UUID.fromString("da52dd55-e1c2-407b-92ef-88eb3941161a"))

  // ZIO's {{Clock}} works with nano resolution but the (Java) internal nano to milli conversion when converting them to
  // {{Instant}]s results in rounding errors. Let's just not test with nanos...
  val zonedDateTime: ZonedDateTime = ZonedDateTime.of(2019, 1, 19, 10, 3, 9, 0, ZoneId.of("Africa/Johannesburg"))

  val event: Event = Event(
    id = eventId,
    title = "title",
    description = "description",
    hostId = userId,
    dateTime = zonedDateTime
  )
  val eventCreationRequest: EventCreationRequest = EventCreationRequest(
    title = event.title,
    description = event.description,
    dateTime = event.dateTime
  )

  val user: User[String] = User(userId, "name", "email", "password")

  val registrationRequest: RegistrationRequest = RegistrationRequest(user.name, user.email, user.passwordHash)

  val loginRequest: LoginRequest = LoginRequest(user.email, user.passwordHash)
  val sessionUser: SessionUser = SessionUser(userId, user.name, user.email)
}
