package eventer.application

import cats.syntax.semigroupk._
import eventer.domain.event.EventRepository
import eventer.domain.{EventCreationRequest, SessionUser}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Task, URLayer, ZLayer}

@accessible
object EventRoutes {
  trait Service {
    def routes: HttpRoutes[Task]
  }

  val live: URLayer[EventRepository with EventIdGenerator with AuthMiddleware, EventRoutes] =
    ZLayer
      .fromServices[EventRepository.Service, EventIdGenerator.Service, AuthMiddleware.Service, Service](
        new EventRoutesImpl(_, _, _))
}

private class EventRoutesImpl(eventRepository: EventRepository.Service,
                              generateEventId: EventIdGenerator.Service,
                              authMiddleware: AuthMiddleware.Service)
    extends EventRoutes.Service
    with Http4sDsl[Task]
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

  override val routes: HttpRoutes[Task] = publicRoutes <+> authMiddleware(privateRoutes)
}
