package eventer.infrastructure

import java.util.UUID

import eventer.domain.{TestData, User, UserId}
import eventer.{TestEnvSpec, dbTestM}
import zio.RIO
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test._

object DbUserRepositorySpec {
  private val userRepository = new DbUserRepository[String](identity, identity)
  private val otherId = UserId(UUID.fromString("b2bb6af3-76b1-4116-944f-5efe2b448357"))
  private val otherEmail = "other-email"
  private val findAll: RIO[DatabaseContext with Blocking, Seq[User[String]]] = withCtx { ctx =>
    import ctx._
    val q = quote(query[User[String]])
    performEffect(runIO(q))
  }

  lazy val spec: TestEnvSpec = suite("DbUserRepository")(
    suite("create")(dbTestM("inserts the given user into the DB") {
      for {
        _ <- userRepository.create(TestData.user)
        foundUsers <- findAll
      } yield assert(foundUsers, equalTo(Seq(TestData.user)))
    }),
    suite("findByEmail")(
      dbTestM("returns the user with the given email address if it exists") {
        for {
          _ <- userRepository.create(TestData.user)
          _ <- userRepository.create(TestData.user.copy(id = otherId, email = otherEmail))
          foundUser <- userRepository.findByEmail(TestData.user.email)
        } yield assert(foundUser, isSome(equalTo(TestData.user)))
      },
      dbTestM("returns nothing if the DB is empty") {
        for {
          foundUser <- userRepository.findByEmail(TestData.user.email)
        } yield assert(foundUser, isNone)
      },
      dbTestM("returns nothing if no event with the given id exists") {
        for {
          _ <- userRepository.create(TestData.user)
          foundUser <- userRepository.findByEmail(otherEmail)
        } yield assert(foundUser, isNone)
      }
    )
  )
}
