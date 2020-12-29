package eventer.infrastructure

import eventer.domain.event.EventRepository
import eventer.domain.{Event, EventId}
import zio.{UIO, URLayer, ZLayer}

object DbEventRepository {

  def live: URLayer[DatabaseContext, EventRepository] = ZLayer.fromService { ctx =>
    import ctx._

    new EventRepository.Service {
      override def create(event: Event): UIO[Unit] = {
        val q = quote(schema.event.insert(lift(DbEvent.fromEvent(event))))
        performEffect_(runIO(q)).orDie
      }

      override val findAll: UIO[Seq[Event]] = {
        val q = quote(schema.event)
        performEffect(runIO(q)).map(_.map(_.toEvent)).orDie
      }

      override def findById(id: EventId): UIO[Option[Event]] = {
        val q = quote(schema.event.filter(_.id == lift(id)))
        performEffect(runIO(q)).map(_.headOption.map(_.toEvent)).orDie
      }
    }
  }

}
