package eventer.application

import eventer.Base64
import eventer.domain.TestData
import eventer.domain.session.InMemorySessionService
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock
import zio.{Task, ZIO}

import java.util.concurrent.TimeUnit

object SessionRoutesSpec extends RoutesSpec {
  val spec: TestEnvSpec = suite("SessionRoutes")(
    suite("POST /")(
      testM("logs in the user if the credentials are correct and sets the session cookie") {
        val duration = Duration.apply(123, TimeUnit.DAYS)
        for {
          // just not start at 0 to avoid bugs when converting between epoch seconds and second durations
          _ <- TestClock.adjust(duration)
          service <- InMemorySessionService.make(Map(TestData.loginRequest -> TestData.sessionUser))
          clock <- ZIO.environment[Clock]
          routes = new SessionRoutes(clock.get, TestJwts, service, neverAuthedMiddleware, useSecureCookies = true)
          request = Request(Method.POST, uri"/").withEntity(TestData.loginRequest)
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
          thirtyDaysInSeconds = 60 * 60 * 24 * 30
        } yield {
          def makeCookie(name: String, content: String, httpOnly: Boolean) =
            ResponseCookie(name, content, maxAge = Some(thirtyDaysInSeconds), secure = true, httpOnly = httpOnly)
          assert(response.status)(equalTo(Status.Created)) &&
          assert(response.cookies)(contains(makeCookie("jwt-signature", "signature", httpOnly = true))) &&
          assert(response.cookies)(
            contains(
              makeCookie("jwt-header.payload",
                         s"header.${Base64.encode(TestData.sessionUser.asJson.noSpaces)}",
                         httpOnly = false))) &&
          assert(body)(isNone)
        }
      },
      testM("rejects the request if the credentials are incorrect") {
        for {
          service <- InMemorySessionService.empty
          clock <- ZIO.environment[Clock]
          routes = new SessionRoutes(clock.get, TestJwts, service, neverAuthedMiddleware, useSecureCookies = true)
          request = Request[Task](Method.POST, uri"/sessions").withEntity(TestData.loginRequest)
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
        } yield
          assert(response.status)(equalTo(Status.Forbidden)) &&
            assert(body)(isNone)
      },
    ),
    suite("DELETE /")(
      testM("deletes the jwt cookies when already logged in") {
        for {
          service <- InMemorySessionService.empty
          clock <- ZIO.environment[Clock]
          routes = new SessionRoutes(clock.get, TestJwts, service, alwaysAuthedMiddleware, useSecureCookies = true)
          request = Request[Task](Method.DELETE, uri"/")
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
        } yield {
          def makeCookie(name: String, httpOnly: Boolean) =
            ResponseCookie(name,
                           "",
                           expires = Some(HttpDate.Epoch),
                           maxAge = Some(0),
                           secure = true,
                           httpOnly = httpOnly)
          assert(response.cookies)(contains(makeCookie("jwt-header.payload", httpOnly = false))) &&
          assert(response.cookies)(contains(makeCookie("jwt-signature", httpOnly = true))) &&
          assert(body)(isNone)
        }
      },
      testM("rejects the request when not logged in") {
        for {
          service <- InMemorySessionService.empty
          clock <- ZIO.environment[Clock]
          routes = new SessionRoutes(clock.get, TestJwts, service, neverAuthedMiddleware, useSecureCookies = true)
          request = Request[Task](Method.DELETE, uri"/")
          response <- routes.routes.run(request).value.someOrFailException
          body <- parseResponseBody[Unit](response)
        } yield assert(response.status)(equalTo(Status.Forbidden)) && assert(body)(isNone)
      }
    )
  )
}
