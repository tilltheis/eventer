package eventer.application

import cats.data.{Kleisli, OptionT}
import eventer.application.WebServer.{JwtHeaderPayloadCookieName, JwtSignatureCookieName}
import eventer.domain.SessionUser
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Middleware}
import org.http4s.{AuthedRequest, Request, Response}
import zio.interop.catz._
import zio.{Task, UIO}

class MiddlewaresImpl[SessionServiceR](jwts: Jwts) extends Middlewares with Codecs[Task] {
//  private[application] val csrf: CSRF[OptionTIO#T, IO] = {
//    // Double submit cookie is enough, no need to check Origin header on top of that.
//    val csrfBuilder: CSRFBuilder[OptionTIO#T, IO] = CSRF(csrfKey, _ => true)
//    csrfBuilder
//      .withCookieName(CsrfTokenCookieName)
//      .withHeaderName(CaseInsensitiveString(CsrfTokenHeaderName))
//      .withCookiePath(None)
//      .withCookieHttpOnly(false)
//      .withCookieSecure(useSecureCookies)
//      .build
//  }

//  type IO[A] = RIO[R with Clock, A]
//  type OptionTIO = { type T[A] = OptionT[IO, A] }

//  type AuthF[A] = RIO[SessionServiceR with Clock, A]

  private def authUser: Kleisli[OptionT[Task, *], Request[Task], SessionUser] =
    Kleisli { request =>
      val sessionUserM = for {
        jwtHeaderPayload <- UIO(request.cookies.find(_.name == JwtHeaderPayloadCookieName).map(_.content)).get
        jwtSignature <- UIO(request.cookies.find(_.name == JwtSignatureCookieName).map(_.content)).get
        jwtHeaderAndPayload <- UIO(Some(jwtHeaderPayload).map(_.split('.')).collect { case Array(x, y) => (x, y) }).get
        (jwtHeader, jwtPayload) = jwtHeaderAndPayload
        contentJson <- jwts.decodedJwtHeaderPayloadSignature(jwtHeader, jwtPayload, jwtSignature)
        sessionUser <- UIO.succeed(io.circe.parser.decode[SessionUser](contentJson).toOption).get
      } yield sessionUser
      OptionT(sessionUserM.option)
    }

  // Per default Http4s returns `Unauthorized` which per spec requires a `WWW-Authenticate` header but Http4s doesn't
  // supply it. That's against the spec and that's not cool. Also, that header doesn't make sense for our form based
  // authentication and so the `Unauthorized` HTTP code is inappropriate. We use `Forbidden` instead.
  override def auth
    : Middleware[OptionT[Task, *], AuthedRequest[Task, SessionUser], Response[Task], Request[Task], Response[Task]] = {
    val dsl = Http4sDsl[Task]
    import dsl._

    AuthMiddleware.noSpider(authUser, _ => Forbidden())
  }

  // Having CSRF is more like an additional safety net because we're only planning to have an SPA and that is
  // practically safe against CSRF attacks. The CSRF attack vector only opens when we allow
  // `application/x-www-form-urlencoded` instead of `application/json`.
  // However, HTTP4S currently ignores the content type when decoding requests as JSON and I don't know how to change
  // that. Until either problem is solved we actually have to have the CSRF token in order to be safe.
//  private val csrfMiddleware: Middleware[OptionTIO#T, Request[IO], Response[IO], Request[IO], Response[IO]] =
//    csrf.validate()
}
