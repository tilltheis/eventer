package eventer.application

import com.typesafe.scalalogging.StrictLogging
import eventer.domain.user.UserRepository2
import eventer.domain.{CryptoHashing, Email, RegistrationRequest, UserId}
import eventer.infrastructure.EmailSender2
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz._
import zio.{Task, UIO}

class UserRoutes[HashT](
    userRepository: UserRepository2[HashT],
    emailSender: EmailSender2,
    cryptoHashing: CryptoHashing[HashT],
    generateUserId: UIO[UserId]
) extends StrictLogging
    with Http4sDsl[Task]
    with Codecs[Task] {
  val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case request @ POST -> Root =>
      // todo: read email message and smtp settings from config
      // todo: also create services instead of doing all logic inside of this webserver
      val result = for {
        registrationRequest <- request.as[RegistrationRequest]
        id <- generateUserId
        passwordHash <- cryptoHashing.hash(registrationRequest.password)
        user = registrationRequest.toUser(id, passwordHash)
        _ <- userRepository.create(user)
        _ <- emailSender.sendEmail(
          Email(
            sender = "eventer@eventer.local",
            recipient = user.email,
            subject = "Welcome to Eventer",
            body =
              s"Welcome, ${user.name}!\nPlease finish your registration by clicking this link: https://localhost:3000/confirm-account"
          ))
        response <- Created()
      } yield response

      val result2 = result.tapBoth(x => UIO(logger.warn(s"could not register user (${x.toString})")),
                                   _ => UIO(logger.info("registration finished")))

      result2.catchAll(_ => Created())
  }
}
