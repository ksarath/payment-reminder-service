package dev.payment.reminder.util.json

import dev.payment.reminder.domain.entities.events.{
  Event,
  PaymentEvent,
  PaymentReceived,
  PaymentReminderPeriodElapsed,
  PaymentReminderSent,
  PaymentRequestSent
}
import dev.payment.reminder.error.KafkaReadError

import io.circe.*
import io.circe.Decoder
import io.circe.generic.auto.*
import io.circe.parser.parse
import io.circe.syntax.*

import zio.kafka.consumer.CommittableRecord

import java.time.Instant

import scala.util.Try

/**
 * Decode a kafka message into a PaymentEvent
 */

def decodeKafkaMessage(
  commitableRecord: CommittableRecord[String, String]
): Either[KafkaReadError, Event] =
  (for
    json    <- parse(commitableRecord.value)
    decoded <- Decoder[Event].decodeJson(json)
  yield decoded).left.map(e => KafkaReadError(s"Error while decoding event from kafka message", e))

/**
 * Event encoder & decoder
 */

private implicit val encodePaymentEvent: Encoder[PaymentEvent] = _.asJson

implicit val encoder: Encoder[Event] = (e: Event) =>
  Json.obj(
    ("happenedOn", Json.fromString(e.happenedOn.toString)),
    ("name", Json.fromString(e.event.getClass.getSimpleName.replaceAll("\\$$", ""))),
    ("event", e.event.asJson)
  )

implicit val decoder: Decoder[Event] = new Decoder[Event]:
  override def apply(c: HCursor): Decoder.Result[Event] =
    for
      happenedOnString <- c
                            .downField("happenedOn")
                            .focus
                            .flatMap(_.asString)
                            .toRight(DecodingFailure("happenedOn", c.history))
      happenedOn <- Try(Instant.parse(happenedOnString)).toEither
                      .fold(e => Left(DecodingFailure(e.toString, c.history)), Right.apply)
      name <- c
                .downField("name")
                .focus
                .flatMap(_.asString)
                .toRight(DecodingFailure("name", c.history))
      eventJson <- c
                     .downField("event")
                     .focus
                     .toRight(DecodingFailure("event", c.history))
      event <-
        name match
          case "PaymentRequestSent" =>
            eventJson.as[PaymentRequestSent]
          case "PaymentReceived" =>
            eventJson.as[PaymentReceived]
          case "PaymentReminderPeriodElapsed" =>
            eventJson.as[PaymentReminderPeriodElapsed]
          case "PaymentReminderSent" =>
            eventJson.as[PaymentReminderSent]
          case other =>
            Left(DecodingFailure(s"unrecognised event $other", c.history))
    yield Event(happenedOn, event)
