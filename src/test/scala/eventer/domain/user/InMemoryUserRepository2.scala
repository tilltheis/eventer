package eventer.domain.user

import eventer.domain.User
import eventer.domain.user.UserRepository2.EmailAlreadyInUse
import zio.{IO, Ref, UIO}

object InMemoryUserRepository2 {
  def empty: UIO[InMemoryUserRepository2] = Ref.make(Set.empty[User[String]]).map(new InMemoryUserRepository2(_))
  def make(events: Set[User[String]]): UIO[InMemoryUserRepository2] =
    Ref.make(events).map(new InMemoryUserRepository2(_))
}

class InMemoryUserRepository2(state: Ref[Set[User[String]]]) extends UserRepository2[String] {
  override def create(user: User[String]): IO[EmailAlreadyInUse.type, Unit] =
    state.update(_ + user).unit

  override def findByEmail(email: String): UIO[Option[User[String]]] =
    state.get.map(_.find(_.email == email))
}
