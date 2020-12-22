package eventer.application

import cats.data.{Kleisli, OptionT}
import eventer.domain.SessionUser
import org.http4s.Request
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CSRF
import org.http4s.server.middleware.CSRF.CSRFBuilder
import org.http4s.server.{AuthMiddleware, HttpMiddleware}
import org.http4s.util.CaseInsensitiveString
import zio.interop.catz._
import zio.{Task, UIO}

import javax.crypto.SecretKey

object Middlewares extends Http4sDsl[Task] with Codecs[Task] {
  val CsrfSigningAlgorithm: String = CSRF.SigningAlgo

  private[application] val CsrfTokenCookieName = "csrf-token"
  private[application] val CsrfTokenHeaderName = "X-Csrf-Token"

  private[application] val JwtSignatureCookieName = "jwt-signature"
  private[application] val JwtHeaderPayloadCookieName = "jwt-header.payload"

  // Having CSRF is more like an additional safety net because we're only planning to have an SPA and that is
  // practically safe against CSRF attacks. The CSRF attack vector only opens when we allow
  // `application/x-www-form-urlencoded` instead of `application/json`.
  // However, HTTP4S currently ignores the content type when decoding requests as JSON and I don't know how to change
  // that. Until either problem is solved we actually have to have the CSRF token in order to be safe.
  def csrf(csrfKey: SecretKey, useSecureCookies: Boolean): HttpMiddleware[Task] = {
    // Double submit cookie is enough, no need to check Origin header on top of that.
    val csrfBuilder: CSRFBuilder[OptionT[Task, *], Task] =
      CSRF(csrfKey, _ => true)

    csrfBuilder
      .withCookieName(CsrfTokenCookieName)
      .withHeaderName(CaseInsensitiveString(CsrfTokenHeaderName))
      .withCookiePath(None)
      .withCookieHttpOnly(false)
      .withCookieSecure(useSecureCookies)
      .build
      .validate()
  }

  // Per default Http4s returns `Unauthorized` which per spec requires a `WWW-Authenticate` header but Http4s doesn't
  // supply it. That's against the spec and that's not cool. Also, that header doesn't make sense for our form based
  // authentication and so the `Unauthorized` HTTP code is inappropriate. We use `Forbidden` instead.
  def auth(jwts: Jwts): AuthMiddleware[Task, SessionUser] = {
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

    AuthMiddleware.noSpider(authUser, _ => Forbidden())
  }
}
