package eventer.domain.session

import eventer.EventerSpec
import eventer.domain.TestData
import eventer.domain.session.SessionService.InvalidCredentials
import eventer.infrastructure.PlaintextCryptoHashing
import eventer.infrastructure.test.InMemoryUserRepository
import zio.test.Assertion.equalTo
import zio.test._

object SessionServiceImplSpec extends EventerSpec {
  private val cryptoHashing = new PlaintextCryptoHashing
  private val userRepositoryM = InMemoryUserRepository.live(Set(TestData.user)).build.useNow.map(_.get)
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
