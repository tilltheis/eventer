package eventer.application

import cats.MonadError
import eventer.domain._
import eventer.infrastructure.InMemoryEventRepository
import eventer.{TestEnvSpec, domain}
import org.http4s._
import org.http4s.implicits._
import zio.clock.Clock
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, URIO, ZIO}
import io.circe.syntax.EncoderOps

object WebServerSpec {
  class Fixture {
    val eventRepository = new InMemoryEventRepository
    val sessionService = new InMemorySessionService
    val webServer = new WebServer(eventRepository, sessionService)

    type R = Clock with InMemoryEventRepository.State with InMemorySessionService.State
    type IO[A] = webServer.IO[A]

    val codecs = new Codecs[IO]

    def makeState(
        eventRepositoryStateM: UIO[InMemoryEventRepository.State] = InMemoryEventRepository.emptyState,
        sessionServiceStateM: UIO[InMemorySessionService.State] = InMemorySessionService.emptyState): URIO[Clock, R] =
      for {
        eventState <- eventRepositoryStateM
        sessionServiceState <- sessionServiceStateM
        envClock <- ZIO.environment[Clock]
      } yield
        new Clock with InMemoryEventRepository.State with InMemorySessionService.State {
          override val clock: Clock.Service[Any] = envClock.clock
          override def eventRepositoryStateRef: Ref[Seq[Event]] = eventState.eventRepositoryStateRef
          override def sessionServiceStateRef: Ref[Map[domain.LoginRequest, LoginResponse]] =
            sessionServiceState.sessionServiceStateRef
        }

    def parseResponseBody[A](response: Response[IO])(implicit F: MonadError[IO, Throwable],
                                                     decoder: EntityDecoder[IO, A]): IO[Either[Throwable, Option[A]]] =
      response.body.compile.toVector.flatMap { bytes =>
        if (bytes.isEmpty) UIO.succeed(Right(None))
        else response.as[A](F, decoder).map(x => Right(Some(x))).catchAll(t => UIO.succeed(Left(t)))
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
        state <- makeState(eventRepositoryStateM = InMemoryEventRepository.makeState(events))
        response <- responseM.provide(state)
        body <- parseResponseBody[Seq[Event]](response).provide(state)
      } yield
        assert(response.status, equalTo(Status.Ok)) &&
          assert(body, isRight(isSome(equalTo(events))))
    }),
    suite("POST /events")(
      testM("inserts the event into the repository") {
        val fixture = new Fixture
        import fixture._
        import codecs._
        val responseM = webServer.routes.run(Request(Method.POST, uri"/events").withEntity(TestData.event))
        for {
          state <- makeState()
          response <- responseM.provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
          finalState <- state.eventRepositoryStateRef.get
        } yield
          assert(response.status, equalTo(Status.Created)) &&
            assert(body, isRight(isNone)) &&
            assert(finalState, equalTo(Seq(TestData.event)))
      }
//      , testM("rejects requests that are not authenticated") {
//        val fixture = new Fixture
//        import fixture._
//        import codecs._
//        val responseM = webServer.routes.run(Request(Method.POST, uri"/events").withEntity(TestData.event))
//        for {
//          state <- makeState()
//          response <- responseM.provide(state)
//        } yield assert(response.status, equalTo(Status.Forbidden))
//      }
    ),
    suite("POST /session")(
      testM("returns the user details if the credentials are correct") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        val responseM = webServer.routes.run(Request(Method.POST, uri"/sessions").withEntity(TestData.loginRequest))

        for {
          state <- makeState(
            sessionServiceStateM =
              InMemorySessionService.makeState(Map(TestData.loginRequest -> TestData.loginResponse)))
          response <- responseM.provide(state)
          responseLoginResponse <- parseResponseBody[LoginResponse](response).provide(state)
          thirtyDaysInSeconds = 60 * 60 * 24 * 30
        } yield {
          def makeCookie(name: String, content: String, httpOnly: Boolean) =
            ResponseCookie(name, content, maxAge = Some(thirtyDaysInSeconds), secure = true, httpOnly = httpOnly)
          assert(response.status, equalTo(Status.Created)) &&
          assert(response.cookies, contains(makeCookie("jwt-signature", "signature", httpOnly = true))) &&
          assert(response.cookies,
                 contains(
                   makeCookie("jwt-header.payload",
                              s"header.${TestData.loginResponse.asJson.noSpaces}",
                              httpOnly = false))) &&
          assert(response.cookies, contains(makeCookie("csrf-token", "csrf-token", httpOnly = false))) &&
          assert(responseLoginResponse, isRight(isSome(equalTo(TestData.loginResponse))))
        }
      },
      testM("rejects the request if the credentials are incorrect") {
        val fixture = new Fixture
        import fixture._
        import codecs._

        val responseM = webServer.routes.run(Request(Method.POST, uri"/sessions").withEntity(TestData.loginRequest))

        for {
          state <- makeState()
          response <- responseM.provide(state)
          body <- parseResponseBody[Unit](response).provide(state)
        } yield
          assert(response.status, equalTo(Status.Forbidden)) &&
            assert(body, isRight(isNone))
      }
    ),
    suite("XXX /unknown")(testM(s"returns 404 NotFound") {
      val fixture = new Fixture
      import fixture._

      checkAllM(Gen.fromIterable(Method.all)) { method =>
        for {
          state <- makeState()
          responseM = webServer.routes.run(Request(method, uri"/unknown"))
          response <- responseM.provide(state)
        } yield assert(response.status, equalTo(Status.NotFound))
      }
    })
  )
}
