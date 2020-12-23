package eventer.infrastructure

import eventer.domain.Email
import zio.{Ref, UIO}

object InMemoryEmailSender {
  def empty: UIO[InMemoryEmailSender] = Ref.make(Seq.empty[Email]).map(new InMemoryEmailSender(_))
  def make(events: Seq[Email]): UIO[InMemoryEmailSender] =
    Ref.make(events).map(new InMemoryEmailSender(_))
}

class InMemoryEmailSender(state: Ref[Seq[Email]]) extends EmailSender {
  override def sendEmail(email: Email): UIO[Unit] = state.update(_ :+ email).unit

  def sentEmails: UIO[Seq[Email]] = state.get
}
