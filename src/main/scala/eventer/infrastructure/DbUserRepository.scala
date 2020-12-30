package eventer.infrastructure

import eventer.domain.User
import eventer.domain.user.UserRepository
import eventer.domain.user.UserRepository.EmailAlreadyInUse
import io.getquill.{EntityQuery, Insert}
import org.postgresql.util.PSQLException
import zio.{Tag, UIO, URLayer, ZIO, ZLayer}

object DbUserRepository {
  def live[HashT: Tag](encodePasswordHash: HashT => String,
                       decodePasswordHash: String => HashT): URLayer[DatabaseContext, UserRepository[HashT]] =
    ZLayer.fromService((ctx: DatabaseContext.Service) =>
      new DbUserRepository(ctx, encodePasswordHash, decodePasswordHash))
}

private class DbUserRepository[HashT](ctx: DatabaseContext.Service,
                                      encodePasswordHash: HashT => String,
                                      decodePasswordHash: String => HashT)
    extends UserRepository.Service[HashT] {
  import ctx._

  private implicit val passwordHashEncoder: MappedEncoding[HashT, String] = MappedEncoding(encodePasswordHash)
  private implicit val passwordHashDecoder: MappedEncoding[String, HashT] = MappedEncoding(decodePasswordHash)

  override def create(user: User[HashT]): zio.IO[EmailAlreadyInUse.type, Unit] = {
    val q: ctx.Quoted[Insert[User[HashT]]] = quote(query[User[HashT]].insert(lift(user)))
    performEffect_(runIO(q)).catchAll {
      case e: PSQLException if e.getSQLState == PostgresSqlState.UniqueViolation && e.getMessage.contains("email") =>
        ZIO.fail(EmailAlreadyInUse)
      case e => ZIO.die(e)
    }
  }

  override def findByEmail(email: String): UIO[Option[User[HashT]]] = {
    val q: ctx.Quoted[EntityQuery[User[HashT]]] = quote(query[User[HashT]].filter(_.email == lift(email)))
    performEffect(runIO(q)).map(_.headOption).orDie
  }

}
