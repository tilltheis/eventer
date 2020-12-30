package eventer.application

import cats.data.OptionT
import org.http4s.dsl.Http4sDsl
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.CSRF
import org.http4s.server.middleware.CSRF.CSRFBuilder
import org.http4s.util.CaseInsensitiveString
import zio.Task
import zio.interop.catz._

import javax.crypto.SecretKey

object Middlewares extends Http4sDsl[Task] with Codecs[Task] {
  val CsrfSigningAlgorithm: String = CSRF.SigningAlgo

  private[application] val CsrfTokenCookieName = "csrf-token"
  private[application] val CsrfTokenHeaderName = "X-Csrf-Token"

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
}
