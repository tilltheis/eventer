package eventer.domain

import eventer.UnitSpec
import zio.test._
import zio.test.Assertion._

object SessionServiceImplSpec {
  class Fixture {
    val userRepository = new InMemoryUserRepository
    val cryptoHashing = new PlaintextCryptoHashing
    val sessionService =
      new SessionServiceImpl[InMemoryUserRepository.State, String](userRepository, cryptoHashing, "jwt-signing-key")
    val stateM = InMemoryUserRepository.makeState(Set(TestData.user))
  }

  val spec: UnitSpec = suite("SessionServiceImpl")(
    suite("login")(
      testM("succeeds for correct credentials") {
        val fixture = new Fixture
        import fixture._
        for {
          maybeLoginResponse <- sessionService.login(TestData.loginRequest).provideM(stateM)
        } yield assert(maybeLoginResponse, isSome(equalTo(TestData.loginResponse)))
      },
      testM("fails for incorrect credentials") {
        val fixture = new Fixture
        import fixture._
        checkAllM(
          Gen.fromIterable(Seq(TestData.loginRequest.copy(email = "wrong-email"),
                               TestData.loginRequest.copy(password = "wrong-password")))) { loginRequest =>
          for {
            maybeLoginResponse <- sessionService.login(loginRequest).provideM(stateM)
          } yield assert(maybeLoginResponse, isNone)
        }
      }
    ))
}
