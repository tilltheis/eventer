package eventer.domain

import zio.URIO

trait UserRepository[-R, HashT] {
  def create(user: User[HashT]): URIO[R, Unit]
  def findByEmail(email: String): URIO[R, Option[User[HashT]]]
}
