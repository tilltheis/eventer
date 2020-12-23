package eventer.application

import eventer.domain.TestData
import eventer.domain.user.InMemoryUserRepository
import eventer.infrastructure.{InMemoryEmailSender, PlaintextCryptoHashing}
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.{Task, UIO}

object UserRoutesSpec extends RoutesSpec {
  val spec: TestEnvSpec = suite("UserRoutes")(
    suite("POST /")(
      testM("inserts the new user into the repository and sends an account confirmation email to them") {
        for {
          repository <- InMemoryUserRepository.empty
          emailSender <- InMemoryEmailSender.empty
          routes = new UserRoutes(repository, emailSender, new PlaintextCryptoHashing, UIO.succeed(TestData.userId))
          request = Request[Task](Method.POST, uri"/").withEntity(TestData.registrationRequest)
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
          finalUserRepoState <- repository.findByEmail(TestData.user.email)
          finalEmailSenderState <- emailSender.sentEmails
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(body)(isNone) &&
            assert(finalUserRepoState)(isSome(equalTo(TestData.user))) &&
            assert(finalEmailSenderState)(hasSize(equalTo(1))) &&
            assert(finalEmailSenderState)(hasFirst(hasField("sender", _.sender, equalTo("eventer@eventer.local")))) &&
            assert(finalEmailSenderState)(hasFirst(hasField("recipient", _.recipient, equalTo(TestData.user.email)))) &&
            assert(finalEmailSenderState)(
              hasFirst(hasField("body", _.body, containsString("https://localhost:3000/confirm-account"))))
      },
      testM("returns created even if a user with the same email address already exists") {
        for {
          repository <- InMemoryUserRepository.make(Set(TestData.user))
          emailSender <- InMemoryEmailSender.empty
          routes = new UserRoutes(repository, emailSender, new PlaintextCryptoHashing, UIO.succeed(TestData.userId))
          request = Request[Task](Method.POST, uri"/").withEntity(TestData.registrationRequest)
          response <- routes.routes.run(request).value.someOrFailException
          finalUserRepoState <- repository.findByEmail(TestData.user.email)
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(finalUserRepoState)(isSome(equalTo(TestData.user)))
      }
    )
  )
}
