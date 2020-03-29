package eventer.domain

import zio.ZIO

trait EmailSender[-R] {
  def sendEmail(email: Email): ZIO[R, Nothing, Unit]
}
