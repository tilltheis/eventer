package eventer.infrastructure

import eventer.infrastructure.DatabaseContext.Service
import io.getquill.{PostgresJdbcContext, SnakeCase}
import zio.Task
import zio.blocking.Blocking

trait DatabaseContext {
  def databaseContext: Service
}

object DatabaseContext {

  class Service(blocking: Blocking) extends PostgresJdbcContext[SnakeCase](SnakeCase, "quill") {
    def performEffect[T](io: IO[T, _]): Task[Result[T]] =
      zio.blocking.blocking(Task(performIO(io))).provide(blocking)

    def performEffect_(io: IO[_, _]): Task[Result[Unit]] =
      performEffect(io).map(_ => ())
  }

}
