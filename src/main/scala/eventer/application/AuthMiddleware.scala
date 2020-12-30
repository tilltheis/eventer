package eventer.application

import cats.data.{Kleisli, OptionT}
import eventer.domain.SessionUser
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware => Http4sAuthMiddleware}
import org.http4s.{AuthedRequest, Request, Response}
import zio.interop.catz._
import zio._

object AuthMiddleware extends Http4sDsl[Task] with Codecs[Task] {
  type Service = Http4sAuthMiddleware[Task, SessionUser]

  def apply(x: Kleisli[OptionT[Task, *], AuthedRequest[Task, SessionUser], Response[Task]])
    : URIO[AuthMiddleware, Kleisli[OptionT[Task, *], Request[Task], Response[Task]]] =
    ZIO.service[Service].map(_(x))

  private[application] val JwtSignatureCookieName = "jwt-signature"
  private[application] val JwtHeaderPayloadCookieName = "jwt-header.payload"

  // Per default Http4s returns `Unauthorized` which per spec requires a `WWW-Authenticate` header but Http4s doesn't
  // supply it. That's against the spec and that's not cool. Also, that header doesn't make sense for our form based
  // authentication and so the `Unauthorized` HTTP code is inappropriate. We use `Forbidden` instead.
  val live: ZLayer[Jwts, Nothing, AuthMiddleware] = ZLayer.fromService[Jwts.Service, Service] { jwts =>
    def authUser: Kleisli[OptionT[Task, *], Request[Task], SessionUser] =
      Kleisli { request =>
        val sessionUserM = for {
          jwtHeaderPayload <- UIO(request.cookies.find(_.name == JwtHeaderPayloadCookieName).map(_.content)).get
          jwtSignature <- UIO(request.cookies.find(_.name == JwtSignatureCookieName).map(_.content)).get
          jwtHeaderAndPayload <- UIO(Some(jwtHeaderPayload).map(_.split('.')).collect { case Array(x, y) => (x, y) })
            .get
          (jwtHeader, jwtPayload) = jwtHeaderAndPayload
          contentJson <- jwts.decodeJwtFromHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
          sessionUser <- UIO.succeed(io.circe.parser.decode[SessionUser](contentJson).toOption).get
        } yield sessionUser
        OptionT(sessionUserM.option)
      }

    Http4sAuthMiddleware.noSpider(authUser, _ => Forbidden())
  }
}
