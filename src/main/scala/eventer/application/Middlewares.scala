package eventer.application

import cats.data.{Kleisli, OptionT}
import eventer.domain.SessionUser
import org.http4s.server.Middleware
import org.http4s.{AuthedRequest, Request, Response}
import zio.RIO

trait Middlewares[AuthR] {
  def auth[R]: Middleware[OptionT[RIO[R with AuthR, *], *],
                          AuthedRequest[RIO[R with AuthR, *], SessionUser],
                          Response[RIO[R with AuthR, *]],
                          Request[RIO[R with AuthR, *]],
                          Response[RIO[R with AuthR, *]]]
}
