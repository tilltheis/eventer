package eventer.application

import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.middleware.CSRF
import zio.Task
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object MiddlewaresSpec extends RoutesSpec {
  private val csrfSuite = {
    val keyString = "DXfXgmx9lLTh+25+VfxOMo+uiRdTm47yHNoVu41/jZFWYeXQY+J6ZRwWByrH59SaKQ5PNiXEv25xakqqm9xwAg=="
    val csrfKey = eventer.util.unsafeSecretKeyFromBase64(keyString, Middlewares.CsrfSigningAlgorithm)

    def makeCsrfRequest(method: Method, uri: Uri): Task[Request[Task]] = {
      CSRF[Task, Task](csrfKey, _ => true).build.generateToken[Task].map { token =>
        Request(method, uri)
          .withHeaders(Header(Middlewares.CsrfTokenHeaderName, CSRF.unlift(token)))
          .addCookie(Middlewares.CsrfTokenCookieName, CSRF.unlift(token))
      }
    }

    suite("csrf")(
      testM("tokens are valid across WebServer instances") {
        val keyString = "DXfXgmx9lLTh+25+VfxOMo+uiRdTm47yHNoVu41/jZFWYeXQY+J6ZRwWByrH59SaKQ5PNiXEv25xakqqm9xwAg=="
        val csrfKey = eventer.util.unsafeSecretKeyFromBase64(keyString, Middlewares.CsrfSigningAlgorithm)

        val middleware1 = Middlewares.csrf(csrfKey, useSecureCookies = true)
        val middleware2 = Middlewares.csrf(csrfKey, useSecureCookies = true)

        val routes = HttpRoutes.pure[Task](Response())

        for {
          request <- makeCsrfRequest(Method.POST, uri"/")
          response1 <- middleware1(routes).run(request).value.someOrFailException
          response2 <- middleware2(routes).run(request).value.someOrFailException
        } yield
          assert(response1.status)(equalTo(Status.Ok)) &&
            assert(response2.status)(equalTo(Status.Ok))
      },
      testM("stores csrf cookie if it does not exist") {
        val middleware = Middlewares.csrf(csrfKey, useSecureCookies = true)
        val routes = HttpRoutes.pure[Task](Response())

        checkAllM(Gen.fromIterable(Method.all.filter(_.isSafe))) { method =>
          val request = Request[Task](method, uri"/")
          for {
            response <- middleware(routes).run(request).value.someOrFailException
            csrfToken = response.cookies.find(_.name == Middlewares.CsrfTokenCookieName).map(_.content)
          } yield assert(csrfToken)(isSome)
        }
      },
      testM("rejects the request if the csrf token is missing") {
        val middleware = Middlewares.csrf(csrfKey, useSecureCookies = true)
        val routes = HttpRoutes.pure[Task](Response())

        checkAllM(Gen.fromIterable(Method.all.filterNot(_.isSafe))) { method =>
          val request = Request[Task](method, uri"/")
          for {
            response <- middleware(routes).run(request).value.someOrFailException
          } yield assert(response.status)(equalTo(Status.Forbidden))
        }
      }
    )
  }

  override def spec: TestEnvSpec = {
    suite("Middlewares")(csrfSuite)
  }
}
