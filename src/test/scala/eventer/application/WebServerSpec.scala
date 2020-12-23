package eventer.application

import org.http4s._
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object WebServerSpec extends RoutesSpec {
  private val routingSuites = {
    def makeRoutes(body: String): HttpRoutes[Task] =
      HttpRoutes.pure(Response(body = fs2.Stream.iterable(body.getBytes("UTF-8"))))

    val webServer = new WebServer(eventRoutes = makeRoutes("events"),
                                  sessionRoutes = makeRoutes("sessions"),
                                  userRoutes = makeRoutes("users"),
                                  csrfMiddleware = alwaysVerifiedCsrfMiddleware)

    val unknownRouteSuite = suite("XXX /unknown")(testM("returns 404 Not Found") {
      checkAllM(Gen.fromIterable(Method.all)) { method =>
        val request = Request[Task](method, uri"/unknown")
        for {
          response <- webServer.routes.run(request)
        } yield assert(response.status)(equalTo(Status.NotFound))
      }
    })

    val knownRouteSuites = Seq("events", "sessions", "users").map { path =>
      suite(s"XXX /${path}")(testM("delegates to the event routes") {
        checkAllM(Gen.fromIterable(Method.all)) { method =>
          val request = Request[Task](method, uri"/events")
          for {
            response <- webServer.routes.run(request)
            body <- response.bodyText.compile.string
          } yield
            assert(response.status)(equalTo(Status.Ok)) &&
              assert(body)(equalTo("events"))
        }
      })
    }

    unknownRouteSuite +: knownRouteSuites
  }

  val spec: TestEnvSpec = suite("WebServer")(
    suite("routing")(routingSuites: _*),
    suite("csrf")(
      testM("rejects requests failing the CSRF check") {
        val webServer = new WebServer(eventRoutes = HttpRoutes.empty,
                                      sessionRoutes = HttpRoutes.empty,
                                      userRoutes = HttpRoutes.empty,
                                      csrfMiddleware = neverVerifiedCsrfMiddleware)

        checkAllM(Gen.fromIterable(Method.all)) { method =>
          val request = Request[Task](method, uri"/")
          for {
            response <- webServer.routes.run(request)
          } yield assert(response.status)(equalTo(Status.Forbidden))
        }
      },
      testM("accepts requests passing the CSRF check") {
        val webServer = new WebServer(eventRoutes = HttpRoutes.empty,
                                      sessionRoutes = HttpRoutes.empty,
                                      userRoutes = HttpRoutes.empty,
                                      csrfMiddleware = alwaysVerifiedCsrfMiddleware)

        checkAllM(Gen.fromIterable(Method.all)) { method =>
          val request = Request[Task](method, uri"/")
          for {
            response <- webServer.routes.run(request)
          } yield assert(response.status)(equalTo(Status.NotFound))
        }
      }
    )
  )
}
