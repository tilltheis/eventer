package eventer.domain

import eventer.domain.InMemoryUserRepository.State
import eventer.domain.UserRepository.EmailAlreadyInUse
import zio.{Has, RIO, Ref, UIO, URIO, ZIO}

class InMemoryUserRepository extends UserRepository[Has[State], String] {
  override def create(user: User[String]): ZIO[Has[State], EmailAlreadyInUse.type, Unit] =
    RIO.accessM[Has[State]](_.get.userRepositoryStateRef.update(_ + user)).unit

  override def findByEmail(email: String): URIO[Has[State], Option[User[String]]] =
    RIO.accessM[Has[State]](_.get.userRepositoryStateRef.get).map(_.find(_.email == email))
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
