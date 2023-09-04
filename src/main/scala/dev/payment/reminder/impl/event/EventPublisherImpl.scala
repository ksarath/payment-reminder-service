package dev.payment.reminder.impl.event

import dev.payment.reminder.domain.entities.events.{identifier, Event}
import dev.payment.reminder.domain.event.EventPublisher
import dev.payment.reminder.error.ApplicationError
import dev.payment.reminder.impl.event.source.kafka.KafkaProducer
import dev.payment.reminder.util.json.encoder

import io.circe.Encoder

import zio.{IO, ZIO}

class EventPublisherImpl(producer: KafkaProducer, topic: String) extends EventPublisher:
  def publish(evt: Event): IO[ApplicationError, Unit] =
    for
      jsonString <- ZIO.succeed(Encoder[Event].apply(evt).spaces2)
      _          <- producer.publish(jsonString, evt.event.identifier, topic)
    yield ()
