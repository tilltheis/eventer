package eventer.domain.user

import eventer.domain.User
import zio.macros.accessible
import zio.{IO, UIO}

@accessible
object UserRepository {
  object EmailAlreadyInUse

  trait Service[HashT] {
    def create(user: User[HashT]): IO[EmailAlreadyInUse.type, Unit]
    def findByEmail(email: String): UIO[Option[User[HashT]]]
  }
}
