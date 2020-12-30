package eventer.application

import cats.data.OptionT
import eventer.Base64
import eventer.application.AuthMiddleware.{JwtHeaderPayloadCookieName, JwtSignatureCookieName}
import eventer.domain.{SessionUser, TestData}
import io.circe.syntax.EncoderOps
import org.http4s.{AuthedRoutes, Request}
import zio.Task
import zio.interop.catz._
import zio.test.Assertion.{equalTo, isNone, isRight, isSome}
import zio.test.{assert, suite, testM}

object AuthMiddlewareSpec extends RoutesSpec {
  private val routes = AuthedRoutes[SessionUser, Task](r => OptionT.liftF(Ok(r.context)))

  override def spec: TestEnvSpec =
    suite("AuthMiddleware")(
      testM("parses user from cookies") {
        val request = Request[Task]()
          .addCookie(JwtHeaderPayloadCookieName, s"header.${Base64.encode(TestData.sessionUser.asJson.noSpaces)}")
          .addCookie(JwtSignatureCookieName, "signature")

        for {
          response <- AuthMiddleware(routes).flatMap(_.run(request).value).someOrFailException
          user <- parseResponseBody[SessionUser](response)
        } yield assert(user)(isSome(isRight(equalTo(TestData.sessionUser))))
      },
      testM("rejects the request if cookie cannot be parsed") {
        val request = Request[Task]()
          .addCookie(JwtHeaderPayloadCookieName, s"header.invalid-payload")
          .addCookie(JwtSignatureCookieName, "signature")

        for {
          response <- AuthMiddleware(routes).flatMap(_.run(request).value).someOrFailException
          user <- parseResponseBody[SessionUser](response)
        } yield assert(user)(isNone)
      },
      testM("rejects the request if cookies are missing") {
        val request = Request[Task]()
          .addCookie(JwtHeaderPayloadCookieName, s"header.${Base64.encode(TestData.sessionUser.asJson.noSpaces)}")

        for {
          response <- AuthMiddleware(routes).flatMap(_.run(request).value).someOrFailException
          user <- parseResponseBody[SessionUser](response)
        } yield assert(user)(isNone)
      }
    ).provideSomeLayer(TestJwts.live >>> AuthMiddleware.live)
}
