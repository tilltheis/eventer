package eventer.application

import cats.syntax.semigroupk._
import eventer.domain.event.EventRepository
import eventer.domain.{EventCreationRequest, EventId, SessionUser}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}
import zio.interop.catz._
import zio.{Task, UIO}

class EventRoutes(eventRepository: EventRepository,
                  generateEventId: UIO[EventId],
                  authMiddleware: AuthMiddleware[Task, SessionUser])
    extends Http4sDsl[Task]
    with Codecs[Task] {

  private val publicRoutes = HttpRoutes.of[Task] {
    case GET -> Root =>
      eventRepository.findAll.flatMap(events => Ok(events))
  }

  private val privateRoutes = AuthedRoutes.of[SessionUser, Task] {
    case request @ POST -> Root as sessionUser =>
      for {
        eventCreationRequest <- request.req.as[EventCreationRequest]
        id <- generateEventId
        event = eventCreationRequest.toEvent(id, sessionUser.id)
        _ <- eventRepository.create(event)
        response <- Created()
      } yield response
  }

  val routes: HttpRoutes[Task] = publicRoutes <+> authMiddleware(privateRoutes)
}