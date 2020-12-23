package eventer.application

import cats.syntax.semigroupk._
import eventer.application.Middlewares.{JwtHeaderPayloadCookieName, JwtSignatureCookieName}
import eventer.domain.session.SessionService
import eventer.domain.{LoginRequest, SessionUser}
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import zio.Task
import zio.clock.Clock
import zio.interop.catz._

class SessionRoutes(clock: Clock.Service,
                    jwts: Jwts,
                    sessionService: SessionService,
                    authMiddleware: AuthMiddleware[Task, SessionUser],
                    useSecureCookies: Boolean)
    extends Http4sDsl[Task]
    with Codecs[Task] {
  private val publicRoutes = HttpRoutes.of[Task] {
    case request @ POST -> Root =>
      def successResponse(sessionUser: SessionUser): Task[Response[Task]] =
        for {
          now <- clock.currentDateTime
          expiresAt = now.plusDays(30)
          expiresInSeconds = expiresAt.toEpochSecond - now.toEpochSecond
          (header, payload, signature) <- jwts.encodeJwtIntoHeaderPayloadSignature(sessionUser.asJson.noSpaces,
                                                                                   expiresAt.toInstant)
          response <- Created()
        } yield {
          def makeCookie(name: String, content: String, httpOnly: Boolean) =
            ResponseCookie(name,
                           content,
                           maxAge = Some(expiresInSeconds),
                           secure = useSecureCookies,
                           httpOnly = httpOnly)
          response
            .addCookie(makeCookie(JwtSignatureCookieName, signature, httpOnly = true))
            .addCookie(makeCookie(JwtHeaderPayloadCookieName, header + "." + payload, httpOnly = false))
        }

      for {
        loginRequest <- request.as[LoginRequest]
        sessionUserOption <- sessionService.login(loginRequest).option
        response <- sessionUserOption.fold(Forbidden())(successResponse)
      } yield response
  }

  private val privateRoutes = AuthedRoutes.of[SessionUser, Task] {
    case DELETE -> Root as _ =>
      def removeCookie(name: String, httpOnly: Boolean) =
        ResponseCookie(name,
                       "",
                       expires = Some(HttpDate.Epoch),
                       maxAge = Some(0),
                       secure = useSecureCookies,
                       httpOnly = httpOnly)
      Ok().map(
        _.addCookie(removeCookie(JwtHeaderPayloadCookieName, httpOnly = false))
          .addCookie(removeCookie(JwtSignatureCookieName, httpOnly = true)))
  }

  val routes: HttpRoutes[Task] = publicRoutes <+> authMiddleware(privateRoutes)
}
