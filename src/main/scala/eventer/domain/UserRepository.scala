package eventer.domain

import eventer.domain.UserRepository.EmailAlreadyInUse
import zio.{URIO, ZIO}

trait UserRepository[-R, HashT] {
  def create(user: User[HashT]): ZIO[R, EmailAlreadyInUse.type, Unit]
  def findByEmail(email: String): URIO[R, Option[User[HashT]]]
}

object UserRepository {
  object EmailAlreadyInUse
}
