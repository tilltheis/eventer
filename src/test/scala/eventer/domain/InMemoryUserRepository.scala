package eventer.domain

import eventer.domain.InMemoryUserRepository.State
import eventer.domain.UserRepository.EmailAlreadyInUse
import zio._

class InMemoryUserRepository extends UserRepository[Has[State], String] {
  override def create(user: User[String]): ZIO[Has[State], EmailAlreadyInUse.type, Unit] =
    RIO.accessM[Has[State]](_.get.stateRef.update(_ + user)).unit

  override def findByEmail(email: String): URIO[Has[State], Option[User[String]]] =
    RIO.accessM[Has[State]](_.get.stateRef.get).map(_.find(_.email == email))
}

object InMemoryUserRepository {
  final case class State(stateRef: Ref[Set[User[String]]])
  object State extends InMemorySetStateCompanion(new State(_))
}
