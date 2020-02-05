package eventer.domain

import zio.RIO

trait UserRepository[-R, HashT] {
  def create(user: User[HashT]): RIO[R, Unit]
  def findByEmail(email: String): RIO[R, Option[User[HashT]]]
}
