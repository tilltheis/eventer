package eventer.domain.session

import eventer.TestEnvSpec
import eventer.domain.session.SessionService.InvalidCredentials
import eventer.domain.user.InMemoryUserRepository
import eventer.domain.TestData
import eventer.infrastructure.PlaintextCryptoHashing
import zio.test.Assertion.equalTo
import zio.test._

object SessionServiceImpl2Spec extends DefaultRunnableSpec {
  private val cryptoHashing = new PlaintextCryptoHashing
  private val userRepositoryM = InMemoryUserRepository.make(Set(TestData.user))
  private val sessionServiceM = userRepositoryM.map(new SessionServiceImpl(_, cryptoHashing))

  val spec: TestEnvSpec = suite("SessionServiceImpl")(
    suite("login")(
      testM("succeeds for correct credentials") {
        for {
          sessionService <- sessionServiceM
          maybeSessionUser <- sessionService.login(TestData.loginRequest)
        } yield assert(maybeSessionUser)(equalTo(TestData.sessionUser))
      },
      testM("fails for incorrect credentials") {
        checkAllM(
          Gen.fromIterable(Seq(TestData.loginRequest.copy(email = "wrong-email"),
                               TestData.loginRequest.copy(password = "wrong-password")))) { loginRequest =>
          for {
            sessionService <- sessionServiceM
            maybeLoginError <- sessionService.login(loginRequest).flip
          } yield assert(maybeLoginError)(equalTo(InvalidCredentials))
        }
      }
    )
  )
}
