package eventer.application

import cats.effect.Sync
import eventer.domain.{Event, EventId, LoginRequest, SessionUser, UserId}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

class Codecs[F[_]: Sync] {
  private implicit val eventIdEncoder: Encoder[EventId] = _.id.toString.asJson
  private implicit val eventIdDecoder: Decoder[EventId] = Decoder.decodeUUID.map(EventId)

  implicit val userIdEncoder: Encoder[UserId] = _.id.toString.asJson
  implicit val userIdDecoder: Decoder[UserId] = Decoder.decodeUUID.map(UserId)

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
  implicit val eventDecoder: Decoder[Event] = deriveDecoder

  implicit val loginRequestEncoder: Encoder[LoginRequest] = deriveEncoder
  implicit val loginRequestDecoder: Decoder[LoginRequest] = deriveDecoder

  implicit val sessionUserEncoder: Encoder[SessionUser] = deriveEncoder
  implicit val sessionUserDecoder: Decoder[SessionUser] = deriveDecoder

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[F, A] = jsonOf[F, A]
  implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
