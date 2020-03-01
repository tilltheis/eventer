package eventer.infrastructure

import eventer.domain.{User, UserRepository}
import io.getquill.MappedEncoding
import zio.URIO
import zio.blocking.Blocking

class DbUserRepository[HashT](encodePasswordHash: HashT => String, decodePasswordHash: String => HashT)
    extends UserRepository[DatabaseContext with Blocking, HashT] {
  private implicit val passwordHashEncoder: MappedEncoding[HashT, String] = MappedEncoding(encodePasswordHash)
  private implicit val passwordHashDecoder: MappedEncoding[String, HashT] = MappedEncoding(decodePasswordHash)

  override def create(user: User[HashT]): URIO[DatabaseContext with Blocking, Unit] = withCtx { ctx =>
    import ctx._
    val q = quote(query[User[HashT]].insert(lift(user)))
    performEffect_(runIO(q)).orDie
  }

  override def findByEmail(email: String): URIO[DatabaseContext with Blocking, Option[User[HashT]]] = withCtx { ctx =>
    import ctx._
    val q = quote(query[User[HashT]].filter(_.email == lift(email)))
    performEffect(runIO(q)).map(_.headOption).orDie
  }
}
