package eventer.domain

import java.time.Instant

import eventer.TestEnvSpec
import io.circe.{Json, parser}
import io.circe.syntax.EncoderOps
import zio.test.Assertion._
import zio.test._

object SessionServiceImplSpec {
  private val userRepository = new InMemoryUserRepository
  private val cryptoHashing = new PlaintextCryptoHashing
  private val sessionService =
    new SessionServiceImpl[InMemoryUserRepository.State, String](userRepository, cryptoHashing, "jwt-signing-key")
  private val loginStateM = InMemoryUserRepository.makeState(Set(TestData.user))

  private val now = Instant.ofEpochSecond(13)
  private val expiresAt = Instant.ofEpochSecond(17)

  val spec: TestEnvSpec = suite("SessionServiceImpl")(
    suite("login")(
      testM("succeeds for correct credentials") {
        for {
          maybeLoginResponse <- sessionService.login(TestData.loginRequest).provideM(loginStateM)
        } yield assert(maybeLoginResponse, isSome(equalTo(TestData.loginResponse)))
      },
      testM("fails for incorrect credentials") {
        checkAllM(
          Gen.fromIterable(Seq(TestData.loginRequest.copy(email = "wrong-email"),
                               TestData.loginRequest.copy(password = "wrong-password")))) { loginRequest =>
          for {
            maybeLoginResponse <- sessionService.login(loginRequest).provideM(loginStateM)
          } yield assert(maybeLoginResponse, isNone)
        }
      }
    ),
    suite("encodedJwtHeaderPayloadSignature")(
      testM("creates the correct jwt header and payload") {
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", now, expiresAt)
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
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", now, expiresAt)
          (_, _, jwtSignature) = jwt
          otherJwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"other content"}""", now, expiresAt)
          (_, _, otherJwtSignature) = otherJwt
        } yield assert(jwtSignature, not(equalTo(otherJwtSignature)))
      }
    ),
    suite("decodedJwtHeaderPayloadSignature")(
      testM("decodes a valid jwt to its payload") {
        val content = """{"content":"content"}"""
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature(content, now, expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- sessionService.decodedJwtHeaderPayloadSignature(jwtHeader,
                                                                               jwtPayload,
                                                                               jwtSignature,
                                                                               expiresAt.minusSeconds(1))
        } yield assert(decodedJwtPayload, isSome(equalTo(content)))
      },
      testM("fails if the signature doesn't match") {
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", now, expiresAt)
          (jwtHeader, jwtPayload, _) = jwt
          otherJwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content2"}""", now, expiresAt)
          (_, _, otherJwtSignature) = otherJwt
          decodedJwtPayload <- sessionService.decodedJwtHeaderPayloadSignature(jwtHeader,
                                                                               jwtPayload,
                                                                               otherJwtSignature,
                                                                               expiresAt.minusSeconds(1))
        } yield assert(decodedJwtPayload, isNone)
      },
      testM("fails if the jwt is expired") {
        for {
          jwt <- sessionService.encodedJwtHeaderPayloadSignature("""{"content":"content"}""", now, expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- sessionService.decodedJwtHeaderPayloadSignature(jwtHeader,
                                                                               jwtPayload,
                                                                               jwtSignature,
                                                                               expiresAt)
        } yield assert(decodedJwtPayload, isNone)
      }
    )
  )
}
