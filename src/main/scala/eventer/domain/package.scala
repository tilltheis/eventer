package eventer

import java.time.ZonedDateTime
import java.util.UUID

import scala.runtime.ScalaRunTime

package object domain {
  final case class EventId(id: UUID) extends AnyVal
  final case class Event(id: EventId, title: String, description: String, hostId: UserId, dateTime: ZonedDateTime)

  final case class UserId(id: UUID) extends AnyVal
  final case class User[HashT](id: UserId, name: String, email: String, passwordHash: HashT)

  final case class LoginRequest(email: String, password: String) {
    override def toString: String = ScalaRunTime._toString(copy(password = "<anonymized>"))
  }
  final case class SessionUser(id: UserId, name: String, email: String)
}
