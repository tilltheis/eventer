package eventer.infrastructure

import eventer.domain.UserRepository.EmailAlreadyInUse
import eventer.domain.{User, UserRepository}
import io.getquill.MappedEncoding
import org.postgresql.util.PSQLException
import zio.blocking.Blocking
import zio.{URIO, ZIO}

class DbUserRepository[HashT](encodePasswordHash: HashT => String, decodePasswordHash: String => HashT)
    extends UserRepository[DatabaseContext with Blocking, HashT] {
  private implicit val passwordHashEncoder: MappedEncoding[HashT, String] = MappedEncoding(encodePasswordHash)
  private implicit val passwordHashDecoder: MappedEncoding[String, HashT] = MappedEncoding(decodePasswordHash)

  override def create(user: User[HashT]): ZIO[DatabaseContext with Blocking, EmailAlreadyInUse.type, Unit] = withCtx {
    ctx =>
      import ctx._
      val q = quote(query[User[HashT]].insert(lift(user)))
      performEffect_(runIO(q)).catchAll {
        case e: PSQLException if e.getSQLState == PostgresSqlState.UniqueViolation && e.getMessage.contains("email") =>
          ZIO.fail(EmailAlreadyInUse)
        case e => ZIO.die(e)
      }
  }

  override def findByEmail(email: String): URIO[DatabaseContext with Blocking, Option[User[HashT]]] = withCtx { ctx =>
    import ctx._
    val q = quote(query[User[HashT]].filter(_.email == lift(email)))
    performEffect(runIO(q)).map(_.headOption).orDie
  }
}
