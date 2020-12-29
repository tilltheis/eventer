package eventer.application

import eventer.application.Jwts.JwtSigningAlgorithm
import eventer.{Base64, EventerSpec}
import io.circe.syntax.EncoderOps
import io.circe.{Json, parser}
import zio.duration.Duration
import zio.test.Assertion.{equalTo, isNone, isSome, not}
import zio.test.environment.TestClock
import zio.test.{assert, suite, testM}

import java.time.Instant
import java.util.concurrent.TimeUnit

object JwtsSpec extends EventerSpec {
  private val keyString = "I57lQ6u3M2SgWzjuqj+tyviRaSpBGsLxcJhprwVEonI="
  private val key = eventer.util.unsafeSecretKeyFromBase64(keyString, JwtSigningAlgorithm)
  private val jwtsM = Jwts.live(key).build.useNow.map(_.get)

  private val expiresAt = Instant.ofEpochSecond(17)

  val spec: TestEnvSpec = suite("SessionServiceImpl")(
    suite("encodedJwtHeaderPayloadSignature")(
      testM("creates the correct jwt header and payload") {
        for {
          jwts <- jwtsM
          _ <- TestClock.adjust(Duration(13, TimeUnit.SECONDS))
          jwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, _) = jwt
          jsonHeader <- Base64.decode(jwtHeader).map(parser.parse).rightOrFail(new RuntimeException)
          jsonPayload <- Base64.decode(jwtPayload).map(parser.parse).rightOrFail(new RuntimeException)
        } yield
          assert(jsonHeader)(equalTo(Json.obj("alg" -> "HS256".asJson, "typ" -> "JWT".asJson))) &&
            assert(jsonPayload)(
              equalTo(Json.obj("iat" -> 13.asJson, "exp" -> 17.asJson, "content" -> "content".asJson)))
      },
      testM("creates a signature that is only valid for the original payload") {
        for {
          jwts <- jwtsM
          jwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (_, _, jwtSignature) = jwt
          otherJwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"other content"}""", expiresAt)
          (_, _, otherJwtSignature) = otherJwt
        } yield assert(jwtSignature)(not(equalTo(otherJwtSignature)))
      }
    ),
    suite("decodedJwtHeaderPayloadSignature")(
      testM("decodes a valid jwt to its payload") {
        val content = """{"content":"content"}"""
        for {
          jwts <- jwtsM
          jwt <- jwts.encodeJwtIntoHeaderPayloadSignature(content, expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- jwts
            .decodeJwtFromHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
            .option
        } yield assert(decodedJwtPayload)(isSome(equalTo(content)))
      },
      testM("fails if the signature doesn't match") {
        for {
          jwts <- jwtsM
          jwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, _) = jwt
          otherJwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"content2"}""", expiresAt)
          (_, _, otherJwtSignature) = otherJwt
          decodedJwtPayload <- jwts
            .decodeJwtFromHeaderPayloadSignature(jwtHeader, jwtPayload, otherJwtSignature)
            .option
        } yield assert(decodedJwtPayload)(isNone)
      },
      testM("fails if the jwt is expired") {
        val duration = Duration.fromJava(java.time.Duration.ofMillis(expiresAt.toEpochMilli))
        for {
          jwts <- jwtsM
          _ <- TestClock.adjust(duration)
          jwt <- jwts.encodeJwtIntoHeaderPayloadSignature("""{"content":"content"}""", expiresAt)
          (jwtHeader, jwtPayload, jwtSignature) = jwt
          decodedJwtPayload <- jwts
            .decodeJwtFromHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
            .option
        } yield assert(decodedJwtPayload)(isNone)
      }
    )
  )
}
