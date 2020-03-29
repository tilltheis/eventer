package eventer.infrastructure

import com.typesafe.scalalogging.StrictLogging
import eventer.domain.{Email, EmailSender}
import eventer.infrastructure.EmailSenderImpl.{Authentication, NoAuthentication, PasswordAuthentication}
import zio.{UIO, ZIO}

class EmailSenderImpl(host: String, port: Int, authentication: Authentication)
    extends EmailSender[Any]
    with StrictLogging {
  override def sendEmail(email: Email): ZIO[Any, Nothing, Unit] = {
    import courier._
    val mailer = authentication match {
      case NoAuthentication => Mailer(host, port)()
      case PasswordAuthentication(username, password) =>
        logger.info(s"authenticating using $username:$password")
        Mailer(host, port)
          .auth(true)
          .as(username, password)
          .startTls(false)()
    }

    logger.info(s"Sending email with subject '${email.subject}'.'")
    ZIO
      .fromFuture { implicit ec =>
        mailer(
          Envelope
            .from(email.sender.addr)
            .to(email.recipient.addr)
            .subject(email.subject)
            .content(Text(email.body)))
      }
      .orDie
      .tap(_ => UIO(logger.info("Email sent.")))
  }
}

object EmailSenderImpl {
  sealed trait Authentication
  case object NoAuthentication extends Authentication
  case class PasswordAuthentication(username: String, password: String) extends Authentication
}
