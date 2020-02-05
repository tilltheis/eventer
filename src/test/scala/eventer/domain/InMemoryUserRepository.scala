package eventer.domain

import eventer.domain.InMemoryUserRepository.State
import zio.{RIO, Ref, UIO}

class InMemoryUserRepository extends UserRepository[State, String] {
  override def create(user: User[String]): RIO[State, Unit] =
    RIO.accessM[State](_.sessionServiceStateRef.update(_ + user)).map(_ => ())

  override def findByEmail(email: String): RIO[State, Option[User[String]]] =
    RIO.accessM[State](_.sessionServiceStateRef.get).map(_.find(_.email == email))
}

object InMemoryUserRepository {
  trait State {
    def sessionServiceStateRef: Ref[Set[User[String]]]
  }

  def makeState(state: Set[User[String]]): UIO[State] = Ref.make(state).map { x =>
    new State {
      override val sessionServiceStateRef: Ref[Set[User[String]]] = x
    }
  }

  def emptyState: UIO[State] = makeState(Set.empty)
}
