package eventer.domain.user

import eventer.domain.User
import eventer.domain.user.UserRepository2.EmailAlreadyInUse
import zio.{IO, UIO}

trait UserRepository2[HashT] {
  def create(user: User[HashT]): IO[EmailAlreadyInUse.type, Unit]
  def findByEmail(email: String): UIO[Option[User[HashT]]]
}

object UserRepository2 {
  object EmailAlreadyInUse
}
