package eventer.infrastructure

import eventer.domain.User
import eventer.domain.user.UserRepository2
import eventer.domain.user.UserRepository2.EmailAlreadyInUse
import io.getquill.{EntityQuery, Insert}
import org.postgresql.util.PSQLException
import zio.{UIO, ZIO}

class DbUserRepository2[HashT](ctx: DatabaseContext.Service,
                               encodePasswordHash: HashT => String,
                               decodePasswordHash: String => HashT)
    extends UserRepository2[HashT] {
  import ctx._

  private implicit val passwordHashEncoder: MappedEncoding[HashT, String] = MappedEncoding(encodePasswordHash)
  private implicit val passwordHashDecoder: MappedEncoding[String, HashT] = MappedEncoding(decodePasswordHash)

  override def create(user: User[HashT]): zio.IO[EmailAlreadyInUse.type, Unit] = {
    val q: ctx.Quoted[Insert[User[HashT]]] = quote(query[User[HashT]].insert(lift(user)))
    performEffect_2(runIO(q)).catchAll {
      case e: PSQLException if e.getSQLState == PostgresSqlState.UniqueViolation && e.getMessage.contains("email") =>
        ZIO.fail(EmailAlreadyInUse)
      case e => ZIO.die(e)
    }
  }

  override def findByEmail(email: String): UIO[Option[User[HashT]]] = {
    val q: ctx.Quoted[EntityQuery[User[HashT]]] = quote(query[User[HashT]].filter(_.email == lift(email)))
    performEffect2(runIO(q)).map(_.headOption).orDie
  }

}
