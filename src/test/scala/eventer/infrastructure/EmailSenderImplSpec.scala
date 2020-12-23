package eventer.infrastructure

import com.icegreen.greenmail.util.{GreenMail, GreenMailUtil, ServerSetupTest}
import eventer.EventerSpec
import eventer.domain.Email
import eventer.infrastructure.EmailSenderImpl.NoAuthentication
import zio.test.Assertion._
import zio.test._

import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage

object EmailSenderImplSpec extends EventerSpec {
  val spec: TestEnvSpec = suite("EmailSenderImpl")(suite("sendEmail")(testM("works") {
    val greenMail = new GreenMail(ServerSetupTest.SMTP)
    greenMail.start()
    val email = Email(sender = "eventer@eventer.local",
                      recipient = "example@example.org",
                      subject = "test subject",
                      body = "test body")
    // unfortunately, i couldn't find a good smtp server for tests that supports smtp auth :/
    val emailSender = new EmailSenderImpl(greenMail.getSmtp.getBindTo, greenMail.getSmtp.getPort, NoAuthentication)
    for {
      _ <- emailSender.sendEmail(email)
      receivedEmails = greenMail.getReceivedMessages.toSeq
    } yield {
      assert(receivedEmails)(hasSize(equalTo(1))) &&
      assert(receivedEmails)(
        hasFirst(
          hasField("sender",
                   (x: MimeMessage) => Option(x.getFrom).map(_.toSeq.map(_.toString)),
                   isSome(equalTo(Seq("eventer@eventer.local")))) &&
            hasField("recipient",
                     (x: MimeMessage) => Option(x.getRecipients(RecipientType.TO)).map(_.toSeq.map(_.toString)),
                     isSome(equalTo(Seq("example@example.org")))) &&
            hasField("subject", (x: MimeMessage) => Option(x.getSubject), isSome(equalTo("test subject"))) &&
            hasField("body", GreenMailUtil.getBody, equalTo("test body"))))
    }
  }))
}
