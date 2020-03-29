package eventer.domain
import eventer.domain.InMemoryEmailSender.State
import zio.{Has, Ref, URIO, ZIO}

class InMemoryEmailSender extends EmailSender[Has[State]] {
  override def sendEmail(email: Email): ZIO[Has[State], Nothing, Unit] =
    URIO.accessM[Has[State]](_.get.stateRef.update(_ :+ email)).unit
}

object InMemoryEmailSender {
  final case class State(stateRef: Ref[Seq[Email]])
  object State extends InMemorySeqStateCompanion(new State(_))
}
