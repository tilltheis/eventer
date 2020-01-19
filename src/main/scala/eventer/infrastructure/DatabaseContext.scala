package eventer.infrastructure

import io.getquill.{PostgresJdbcContext, SnakeCase}
import zio.Task
import zio.blocking.Blocking

class DatabaseContext(blocking: Blocking) extends PostgresJdbcContext[SnakeCase](SnakeCase, "quill") {
  def performEffect[T](io: IO[T, _]): Task[Result[T]] =
    zio.blocking.blocking(Task(performIO(io))).provide(blocking)

  def performEffect_(io: IO[_, _]): Task[Result[Unit]] =
    performEffect(io).map(_ => ())
}
