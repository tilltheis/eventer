package eventer.infrastructure

import eventer.domain.Email
import zio.{Ref, UIO}

object InMemoryEmailSender2 {
  def empty: UIO[InMemoryEmailSender2] = Ref.make(Seq.empty[Email]).map(new InMemoryEmailSender2(_))
  def make(events: Seq[Email]): UIO[InMemoryEmailSender2] =
    Ref.make(events).map(new InMemoryEmailSender2(_))
}

class InMemoryEmailSender2(state: Ref[Seq[Email]]) extends EmailSender2 {
  override def sendEmail(email: Email): UIO[Unit] = state.update(_ :+ email).unit

  def sentEmails: UIO[Seq[Email]] = state.get
}
