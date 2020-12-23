package eventer.infrastructure

import eventer.domain.Email
import zio.UIO

trait EmailSender {
  def sendEmail(email: Email): UIO[Unit]
}
