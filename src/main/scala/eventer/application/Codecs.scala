package eventer.application

import java.util.UUID

import cats.effect.Sync
import eventer.domain.{Event, EventId}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import scala.util.Try

class Codecs[F[_]: Sync] {
  private implicit val eventIdEncoder: Encoder[EventId] = _.id.toString.asJson
  private implicit val eventIdDecoder: Decoder[EventId] =
    Decoder.decodeString.emapTry(string => Try(UUID.fromString(string))).map(EventId)

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
  implicit val decodeEvent: Decoder[Event] = deriveDecoder

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[F, A] = jsonOf[F, A]
  implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
