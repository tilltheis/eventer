package eventer.domain

import java.time.Instant
import java.util.concurrent.TimeUnit

import eventer.TestEnvSpec
import eventer.domain.SessionService.InvalidCredentials
import io.circe.syntax.EncoderOps
import io.circe.{Json, parser}
import zio.Has
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock

object SessionServiceImplSpec {
  private val userRepository = new InMemoryUserRepository
  private val cryptoHashing = new PlaintextCryptoHashing
  private val keyString = "I57lQ6u3M2SgWzjuqj+tyviRaSpBGsLxcJhprwVEonI="
  private val key = eventer.util.unsafeSecretKeyFromBase64(keyString, SessionServiceImpl.JwtSigningAlgorithm)
  private val sessionService = new SessionServiceImpl(userRepository, cryptoHashing, key)
  private val loginStateM = InMemoryUserRepository.makeState(Set(TestData.user)).map(Has(_))

  private val now = Duration(13, TimeUnit.SECONDS)
  private val expiresAt = Instant.ofEpochSecond(17)

  val spec: TestEnvSpec = suite("SessionServiceImpl")(
    suite("login")(
      testM("succeeds for correct credentials") {
        for {
          maybeSessionUser <- sessionService.login(TestData.loginRequest).provideM(loginStateM)
        } yield assert(maybeSessionUser, equalTo(TestData.sessionUser))
      },
      testM("fails for incorrect credentials") {
        checkAllM(
          Gen.fromIterable(Seq(TestData.loginRequest.copy(email = "wrong-email"),
                               TestData.loginRequest.copy(password = "wrong-password")))) { loginRequest =>
          for {
            maybeSessionUser <- sessionService.login(loginRequest).provideM(loginStateM).flip
          } yield assert(maybeSessionUser, equalTo(InvalidCredentials))
        }
      }
    ),
    suite("encodedJwtHeaderPayloadSignature")(
      testM("creates the correct jwt header and payload") {
        for {
          _ <- TestClock.adjust(now)
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, _) = jwt
          jsonHeader <- eventer.base64Decode(jwtHeader).map(parser.parse).rightOrFail(new RuntimeException)
          jsonPayload <- eventer.base64Decode(jwtPayload).map(parser.parse).rightOrFail(new RuntimeException)
        } yield
          assert(jsonHeader, equalTo(Json.obj("alg" -> "HS256".asJson, "typ" -> "JWT".asJson))) &&
            assert(jsonPayload,
                   equalTo(Json.obj("iat" -> 13.asJson, "exp" -> 17.asJson, "content" -> "content".asJson)))
      },
      testM("creates a signature that is only valid for the original payload") {
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (_, _, jwtSignature) = jwt
          otherJwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"other content"}""", expiresAt)
          (_, _, otherJwtSignature) = otherJwt
        } yield assert(jwtSignature, not(equalTo(otherJwtSignature)))
      }
    ),
    suite("decodedJwtHeaderPayloadSignature")(
      testM("decodes a valid jwt to its payload") {
        val content = """{"content":"content"}"""
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature(content, expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- sessionService
            .decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
            .option
        } yield assert(decodedJwtPayload, isSome(equalTo(content)))
      },
      testM("fails if the signature doesn't match") {
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, _) = jwt
          otherJwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content2"}""", expiresAt)
          (_, _, otherJwtSignature) = otherJwt
          decodedJwtPayload <- sessionService
            .decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, otherJwtSignature)
            .option
        } yield assert(decodedJwtPayload, isNone)
      },
      testM("fails if the jwt is expired") {
        for {
          _ <- TestClock.adjust(Duration.fromJava(java.time.Duration.ofMillis(expiresAt.toEpochMilli)))
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- sessionService
            .decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
            .option
        } yield assert(decodedJwtPayload, isNone)
      }
    )
  )
}
