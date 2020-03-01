package eventer.domain

import eventer.domain.InMemoryUserRepository.State
import zio.{RIO, Ref, UIO, URIO}

class InMemoryUserRepository extends UserRepository[State, String] {
  override def create(user: User[String]): URIO[State, Unit] =
    RIO.accessM[State](_.userRepositoryStateRef.update(_ + user)).unit

  override def findByEmail(email: String): URIO[State, Option[User[String]]] =
    RIO.accessM[State](_.userRepositoryStateRef.get).map(_.find(_.email == email))
}

object InMemoryUserRepository {
  trait State {
    def userRepositoryStateRef: Ref[Set[User[String]]]
  }

  def makeState(state: Set[User[String]]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val userRepositoryStateRef: Ref[Set[User[String]]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Set.empty)
}
