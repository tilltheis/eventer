package eventer.infrastructure.test

import eventer.domain.User
import eventer.domain.user.UserRepository
import eventer.domain.user.UserRepository.EmailAlreadyInUse
import zio.macros.accessible
import zio._

@accessible
object InMemoryUserRepository {
  trait Service extends UserRepository.Service[String]

  val empty: ULayer[UserRepository[String] with InMemoryUserRepository] = live(Set.empty)

  def live(events: Set[User[String]]): ULayer[UserRepository[String] with InMemoryUserRepository] =
    (for {
      state <- Ref.make(events)
      repo = new InMemoryUserRepositoryServiceImpl(state)
    } yield Has.allOf(repo: UserRepository.Service[String], repo: Service)).toLayerMany
}

private class InMemoryUserRepositoryServiceImpl(state: Ref[Set[User[String]]]) extends InMemoryUserRepository.Service {
  override def create(user: User[String]): IO[EmailAlreadyInUse.type, Unit] =
    state.update(_ + user).unit

  override def findByEmail(email: String): UIO[Option[User[String]]] =
    state.get.map(_.find(_.email == email))
}
