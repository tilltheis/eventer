package eventer.infrastructure

import eventer.domain.user.UserRepository.EmailAlreadyInUse
import eventer.domain.{TestData, User, UserId}
import eventer.{TestEnvSpec, dbTestM}
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import java.util.UUID

object DbUserRepositorySpec extends DbSpec {
  private val otherId = UserId(UUID.fromString("b2bb6af3-76b1-4116-944f-5efe2b448357"))
  private val otherEmail = "other-email"

  private val userRepositoryM =
    ZIO.service[DatabaseContext.Service].map(new DbUserRepository[String](_, identity, identity))

  private val findAll = ZIO.service[DatabaseContext.Service].flatMap { ctx =>
    import ctx._
    val q = quote(query[User[String]])
    performEffect(runIO(q)).orDie
  }

  lazy val dbSpec: TestEnvSpec = suite("DbUserRepository")(
    suite("create")(
      dbTestM("inserts the given user into the DB") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          foundUsers <- findAll
        } yield assert(foundUsers)(equalTo(Seq(TestData.user)))
      },
      dbTestM("fails if an account with the same email already exists") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          insertError <- userRepository.create(TestData.user.copy(id = otherId)).flip
        } yield assert(insertError)(equalTo(EmailAlreadyInUse))
      }
    ),
    suite("findByEmail")(
      dbTestM("returns the user with the given email address if it exists") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          _ <- userRepository.create(TestData.user.copy(id = otherId, email = otherEmail))
          foundUser <- userRepository.findByEmail(TestData.user.email)
        } yield assert(foundUser)(isSome(equalTo(TestData.user)))
      },
      dbTestM("returns nothing if the DB is empty") {
        for {
          userRepository <- userRepositoryM
          foundUser <- userRepository.findByEmail(TestData.user.email)
        } yield assert(foundUser)(isNone)
      },
      dbTestM("returns nothing if no event with the given id exists") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          foundUser <- userRepository.findByEmail(otherEmail)
        } yield assert(foundUser)(isNone)
      }
    )
  )
}
