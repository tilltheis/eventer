package eventer.infrastructure

import eventer.domain.Email
import zio.UIO

trait EmailSender2 {
  def sendEmail(email: Email): UIO[Unit]
}
