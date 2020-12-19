package eventer.application

import java.util.concurrent.TimeUnit

import cats.MonadError
import cats.instances.option._
import cats.syntax.traverse._
import eventer.{TestEnvSpec, domain}
import eventer.domain.{InMemoryEventRepository, _}
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.CSRF
import zio._
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock

object WebServerSpec {
  class Fixture {
    trait CsrfTokenGeneratorState { val csrfTokenGeneratorStateRef: Ref[Iterator[String]] }
    val eventRepository = new InMemoryEventRepository
    val sessionService = new InMemorySessionService
    val userRepository = new InMemoryUserRepository
    val emailSender = new InMemoryEmailSender
    val cryptoHashing = new PlaintextCryptoHashing
    private val keyString = "DXfXgmx9lLTh+25+VfxOMo+uiRdTm47yHNoVu41/jZFWYeXQY+J6ZRwWByrH59SaKQ5PNiXEv25xakqqm9xwAg=="
    val csrfKey = eventer.util.unsafeSecretKeyFromBase64(keyString, WebServer.CsrfSigningAlgorithm)

    type R = Clock
      with Has[InMemoryEventRepository.State]
      with Has[InMemorySessionService.State]
      with Has[InMemoryUserRepository.State]
      with Has[InMemoryEmailSender.State]

    val webServer = new WebServer(
      eventRepository,
      sessionService,
      userRepository,
      emailSender,
      cryptoHashing,
      UIO.succeed(TestData.eventId),
      UIO.succeed(TestData.userId),
      csrfKey,
      useSecureCookies = true
    )

    type IO[A] = webServer.IO[A]

    val codecs = new Codecs[IO]
    import codecs._

    def CsrfRequestM(method: Method, uri: Uri): Task[Request[IO]] =
      webServer.csrf.generateToken[Task].map { token =>
        Request(method, uri)
          .withHeaders(Header(WebServer.CsrfTokenHeaderName, CSRF.unlift(token)))
          .addCookie(WebServer.CsrfTokenCookieName, CSRF.unlift(token))
      }

    def AuthedRequestM(method: Method, uri: Uri): Task[Request[IO]] =
      CsrfRequestM(method, uri).map(
        _.addCookie("jwt-header.payload", s"header.${eventer.base64Encode(TestData.sessionUser.asJson.noSpaces)}")
          .addCookie("jwt-signature", "signature"))

    def makeState(eventRepositoryStateM: UIO[InMemoryEventRepository.State] = InMemoryEventRepository.State.empty,
                  sessionServiceStateM: UIO[InMemorySessionService.State] = InMemorySessionService.State.empty,
                  userRepositoryStateM: UIO[InMemoryUserRepository.State] = InMemoryUserRepository.State.empty,
                  emailSenderStateM: UIO[InMemoryEmailSender.State] = InMemoryEmailSender.State.empty): URIO[Clock, R] =
      ZIO
        .environment[R]
        .provideLayer(
          ZLayer.requires[Clock] ++
            ZLayer.fromEffect(eventRepositoryStateM) ++
            ZLayer.fromEffect(sessionServiceStateM) ++
            ZLayer.fromEffect(userRepositoryStateM) ++
            ZLayer.fromEffect(emailSenderStateM))

    def parseResponseBody[A](response: Response[IO])(implicit F: MonadError[IO, Throwable],
                                                     decoder: EntityDecoder[IO, A]): IO[Option[Either[Throwable, A]]] =
      response.body.compile.toVector.flatMap { bytes =>
        Option
          .when(bytes.nonEmpty)(response)
          .traverse(_.as[A](F, decoder).map(Right(_)).catchAll(t => UIO.succeed(Left(t))))
      }
  }

  val spec: TestEnvSpec = suite("WebServer")(
    suite("GET /events")(testM("returns the events from the repository") {
      val fixture = new Fixture
      import fixture._
      import codecs._

      val events = Seq(TestData.event, TestData.event)
      val responseM = webServer.routes.run(Request(Method.GET, uri"/events"))
      for {
        state <- makeState(eventRepositoryStateM = InMemoryEventRepository.State.make(events))
        response <- responseM.provide(state)
        body <- parseResponseBody[Seq[Event]](response).provide(state)
      } yield
        assert(response.status)(equalTo(Status.Ok)) &&
          assert(body)(isSome(isRight(equalTo(events))))
    }),
    suite("csrf")(
      testM("tokens are valid across WebServer instances") {
        val fixture = new Fixture
        import fixture._

        val webServer2 = new WebServer(
          eventRepository,
          sessionService,
          userRepository,
          emailSender,
          cryptoHashing,
          UIO.succeed(TestData.eventId),
          UIO.succeed(TestData.userId),
          csrfKey,
          useSecureCookies = true
        )

        for {
          state <- makeState()
          request <- CsrfRequestM(Method.GET, uri"/events")
          response <- webServer.routes.run(request).provide(state)
          response2 <- webServer2.routes.run(request).provide(state)
        } yield assert(response.status)(equalTo(Status.Ok)) && assert(response2.status)(equalTo(Status.Ok))
      }
    ),
    suite("POST /events")(
      testM("inserts the event into the repository") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState()
          request <- AuthedRequestM(Method.POST, uri"/events").map(_.withEntity(TestData.eventCreationRequest))
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
          finalState <- state.get[InMemoryEventRepository.State].stateRef.get
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(body)(isNone) &&
            assert(finalState)(equalTo(Seq(TestData.event)))
      },
      testM("rejects requests that are not authenticated") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        val responseM = webServer.routes.run(Request(Method.POST, uri"/events").withEntity(TestData.event))
        for {
          state <- makeState()
          response <- responseM.provide(state)
        } yield assert(response.status)(equalTo(Status.Forbidden))
      }
    ),
    suite("POST /users")(
      testM("inserts the new user into the repository and sends an account confirmation email to them") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState()
          request <- CsrfRequestM(Method.POST, uri"/users").map(_.withEntity(TestData.registrationRequest))
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
          finalUserRepoState <- state.get[InMemoryUserRepository.State].stateRef.get
          finalEmailSenderState <- state.get[InMemoryEmailSender.State].stateRef.get
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(body)(isNone) &&
            assert(finalUserRepoState)(equalTo(Set(TestData.user))) &&
            assert(finalEmailSenderState)(hasSize(equalTo(1))) &&
            assert(finalEmailSenderState)(hasFirst(hasField("sender", _.sender, equalTo("eventer@eventer.local")))) &&
            assert(finalEmailSenderState)(hasFirst(hasField("recipient", _.recipient, equalTo(TestData.user.email)))) &&
            assert(finalEmailSenderState)(
              hasFirst(hasField("body", _.body, containsString("https://localhost:3000/confirm-account"))))
      },
      testM("returns created even if a user with the same email address already exists") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState(userRepositoryStateM = InMemoryUserRepository.State.make(Set(TestData.user)))
          request <- CsrfRequestM(Method.POST, uri"/users").map(_.withEntity(TestData.registrationRequest))
          response <- webServer.routes.run(request).provide(state)
          finalUserRepoState <- state.get[InMemoryUserRepository.State].stateRef.get
        } yield
          assert(response.status)(equalTo(Status.Created)) &&
            assert(finalUserRepoState)(equalTo(Set(TestData.user)))
      }
    ),
    suite("POST /session")(
      testM("logs in the user if the credentials are correct and sets the session cookie") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        val duration = Duration.apply(123, TimeUnit.DAYS)
        for {
          // just not start at 0 to avoid bugs when converting between epoch seconds and second durations
          _ <- TestClock.adjust(duration)
          state <- makeState(sessionServiceStateM =
            InMemorySessionService.State.make(Map(TestData.loginRequest -> TestData.sessionUser)))
          request <- CsrfRequestM(Method.POST, uri"/sessions").map(_.withEntity(TestData.loginRequest))
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
          thirtyDaysInSeconds = 60 * 60 * 24 * 30
        } yield {
          def makeCookie(name: String, content: String, httpOnly: Boolean) =
            ResponseCookie(name, content, maxAge = Some(thirtyDaysInSeconds), secure = true, httpOnly = httpOnly)
          assert(response.status)(equalTo(Status.Created)) &&
          assert(response.cookies)(contains(makeCookie("jwt-signature", "signature", httpOnly = true))) &&
          assert(response.cookies)(
            contains(
              makeCookie("jwt-header.payload",
                         s"header.${eventer.base64Encode(TestData.sessionUser.asJson.noSpaces)}",
                         httpOnly = false))) &&
          assert(body)(isNone)
        }
      },
      testM("generates a random csrf token") {
        // This should be tested for every route but since our middleware takes care of that and i don't want to bloat
        // the tests it should be good enough to only test csrf token generation for this route.

        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState(sessionServiceStateM =
            InMemorySessionService.State.make(Map(TestData.loginRequest -> TestData.sessionUser)))
          request <- CsrfRequestM(Method.POST, uri"/sessions").map(_.withEntity(TestData.loginRequest))
          response1 <- webServer.routes.run(request).provide(state)
          response2 <- webServer.routes.run(request).provide(state)
          csrfToken1 <- UIO(response1.cookies.find(_.name == WebServer.CsrfTokenCookieName).map(_.content))
          csrfToken2 <- UIO(response2.cookies.find(_.name == WebServer.CsrfTokenCookieName).map(_.content))
        } yield
          assert(csrfToken1)(not(isNone)) && assert(csrfToken2)(not(isNone)) &&
            assert(csrfToken1)(not(equalTo(csrfToken2)))
      },
      testM("rejects the request if the credentials are incorrect") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState()
          request <- CsrfRequestM(Method.POST, uri"/sessions").map(_.withEntity(TestData.loginRequest))
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
        } yield
          assert(response.status)(equalTo(Status.Forbidden)) &&
            assert(body)(isNone)
      },
      testM("rejects the request if the csrf token is missing") {
        // This should be tested for every route but since our middleware takes care of that and i don't want to bloat
        // the tests it should be good enough to only test csrf token generation for this route.

        val fixture = new Fixture
        import fixture._
        import codecs._

        val responseM = webServer.routes.run(Request(Method.POST, uri"/sessions").withEntity(TestData.loginRequest))

        for {
          state <- makeState(sessionServiceStateM =
            InMemorySessionService.State.make(Map(TestData.loginRequest -> TestData.sessionUser)))
          response <- responseM.provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
        } yield
          assert(response.status)(equalTo(Status.Forbidden)) &&
            assert(body)(isNone)
      }
    ),
    suite("DELETE /session")(
      testM("deletes the jwt cookies when already logged in") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState()
          request <- AuthedRequestM(Method.DELETE, uri"/sessions")
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
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
        val fixture = new Fixture
        import fixture._
        import codecs._

        for {
          state <- makeState()
          request <- CsrfRequestM(Method.DELETE, uri"/sessions")
          response <- webServer.routes.run(request).provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
        } yield assert(response.status)(equalTo(Status.Forbidden)) && assert(body)(isNone)
      }
    ),
    suite("XXX /unknown")(testM(s"returns 404 Not Found") {
      val fixture = new Fixture
      import fixture._

      checkAllM(Gen.fromIterable(Method.all)) { method =>
        for {
          state <- makeState()
          request <- CsrfRequestM(method, uri"/unknown")
          responseM = webServer.routes.run(request)
          response <- responseM.provide(state)
        } yield assert(response.status)(equalTo(Status.NotFound))
      }
    })
  )
}
