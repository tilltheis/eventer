package eventer.application

import cats.effect.Sync
import eventer.domain._
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

class Codecs[F[_]: Sync] {
  private implicit val eventIdEncoder: Encoder[EventId] = _.id.toString.asJson
  private implicit val eventIdDecoder: Decoder[EventId] = Decoder.decodeUUID.map(EventId)

  implicit val userIdEncoder: Encoder[UserId] = _.id.toString.asJson
  implicit val userIdDecoder: Decoder[UserId] = Decoder.decodeUUID.map(UserId)

  implicit val eventCodec: Codec[Event] = deriveCodec
  implicit val eventCreationRequestCodec: Codec[EventCreationRequest] = deriveCodec
  implicit val registrationRequestCodec: Codec[RegistrationRequest] = deriveCodec
  implicit val loginRequestCodec: Codec[LoginRequest] = deriveCodec
  implicit val sessionUserCodec: Codec[SessionUser] = deriveCodec

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[F, A] = jsonOf[F, A]
  implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
