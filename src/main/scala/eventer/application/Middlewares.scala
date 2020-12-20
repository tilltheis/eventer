package eventer.application

import cats.data.OptionT
import eventer.domain.SessionUser
import org.http4s.server.Middleware
import org.http4s.{AuthedRequest, Request, Response}
import zio.Task

trait Middlewares {
  def auth
    : Middleware[OptionT[Task, *], AuthedRequest[Task, SessionUser], Response[Task], Request[Task], Response[Task]]
}
