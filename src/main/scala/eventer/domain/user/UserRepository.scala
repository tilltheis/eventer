package eventer.domain.user

import eventer.domain.User
import eventer.domain.user.UserRepository.EmailAlreadyInUse
import zio.{IO, UIO}

object UserRepository {
  object EmailAlreadyInUse
}

trait UserRepository[HashT] {
  def create(user: User[HashT]): IO[EmailAlreadyInUse.type, Unit]
  def findByEmail(email: String): UIO[Option[User[HashT]]]
}
