package eventer.infrastructure

import eventer.EventerSpec
import zio.test.Assertion._
import zio.test._

object BlowfishCryptoHashingSpec extends EventerSpec {
  val spec: TestEnvSpec =
    suite("BlowfishCryptoHashing")(
      suite("verify")(
        testM("returns true for the original hash input") {
          val blowfish = new BlowfishCryptoHashing
          checkNM(10)(Gen.anyString) { x =>
            for {
              hashed <- blowfish.hash(x)
              verified <- blowfish.verify(x, hashed)
            } yield assert(verified)(isTrue)
          }
        },
        testM("returns false for anything that is not the original hash input") {
          val blowfish = new BlowfishCryptoHashing
          for {
            hashed <- blowfish.hash("foo")
            verified <- blowfish.verify("bar", hashed)
          } yield assert(verified)(isFalse)
        }
      ),
      suite("BlowfishHash.unsafeFromHashString")(
        test("succeeds for valid hashes") {
          assert(
            BlowfishCryptoHashing.BlowfishHash.unsafeFromHashString(
              "$2a$10$d.vQEHwPIqtSYWQOMtg7LuZgTOx1R/2sOLnqCUkpixkXJ1paUhEIm"))(
            not(throwsA[RuntimeException])
          )
        },
        test("fails for invalid hashes") {
          assert(BlowfishCryptoHashing.BlowfishHash.unsafeFromHashString("foo"))(throwsA[RuntimeException])
        }
      )
    )
}
