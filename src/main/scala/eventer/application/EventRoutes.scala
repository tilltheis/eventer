package eventer.application

import cats.syntax.semigroupk._
import eventer.domain.{EventCreationRequest, EventId, EventRepository, SessionUser}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import zio.interop.catz._
import zio.{RIO, UIO}

class EventRoutes[EventRepoR, AuthMiddlewareR](eventRepository: EventRepository[EventRepoR],
                                               generateEventId: UIO[EventId],
                                               middlewares: Middlewares[AuthMiddlewareR])
    extends Http4sDsl[RIO[EventRepoR with AuthMiddlewareR, *]]
    with Codecs[RIO[EventRepoR with AuthMiddlewareR, *]] {

  // The type must match `privateRoutes` so that `<+>` can be used even though this val doesn't need a `Clock`.
  private val publicRoutes = HttpRoutes.of[RIO[EventRepoR with AuthMiddlewareR, *]] {
    case GET -> Root =>
      eventRepository.findAll.flatMap(events => Ok(events))
  }

  private val privateRoutes = AuthedRoutes.of[SessionUser, RIO[EventRepoR with AuthMiddlewareR, *]] {
    case request @ POST -> Root as sessionUser =>
      for {
        eventCreationRequest <- request.req.as[EventCreationRequest]
        id <- generateEventId
        event = eventCreationRequest.toEvent(id, sessionUser.id)
        _ <- eventRepository.create(event)
        response <- Created()
      } yield response
  }

  val routes: HttpRoutes[RIO[EventRepoR with AuthMiddlewareR, *]] =
    publicRoutes <+> middlewares.auth[EventRepoR](privateRoutes)
}
