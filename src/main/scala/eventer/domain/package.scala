package eventer

import java.time.ZonedDateTime
import java.util.UUID

import scala.runtime.ScalaRunTime

package object domain {
  final case class EventId(id: UUID) extends AnyVal
  final case class Event(id: EventId, title: String, description: String, hostId: UserId, dateTime: ZonedDateTime)

  final case class UserId(id: UUID) extends AnyVal
  final case class User[HashT](id: UserId, name: String, email: String, passwordHash: HashT) {
    def toSessionUser: SessionUser = SessionUser(id, name, email)
  }

  final case class RegistrationRequest(name: String, email: String, password: String) {
    def toUser[HashT](id: UserId, passwordHash: HashT): User[HashT] = User(id, name, email, passwordHash)
  }

  final case class LoginRequest(email: String, password: String) {
    override def toString: String = ScalaRunTime._toString(copy(password = "<anonymized>"))
  }
  final case class SessionUser(id: UserId, name: String, email: String)

  final case class EventCreationRequest(title: String, description: String, dateTime: ZonedDateTime) {
    def toEvent(id: EventId, hostId: UserId): Event =
      Event(id = id, title = title, description = description, hostId = hostId, dateTime = dateTime)
  }

  final case class Email(sender: String, recipient: String, subject: String, body: String)
}
