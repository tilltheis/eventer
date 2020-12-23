package eventer.domain.user

import eventer.domain.User
import eventer.domain.user.UserRepository.EmailAlreadyInUse
import zio.{IO, Ref, UIO}

object InMemoryUserRepository {
  def empty: UIO[InMemoryUserRepository] = Ref.make(Set.empty[User[String]]).map(new InMemoryUserRepository(_))
  def make(events: Set[User[String]]): UIO[InMemoryUserRepository] =
    Ref.make(events).map(new InMemoryUserRepository(_))
}

class InMemoryUserRepository(state: Ref[Set[User[String]]]) extends UserRepository[String] {
  override def create(user: User[String]): IO[EmailAlreadyInUse.type, Unit] =
    state.update(_ + user).unit

  override def findByEmail(email: String): UIO[Option[User[String]]] =
    state.get.map(_.find(_.email == email))
}
