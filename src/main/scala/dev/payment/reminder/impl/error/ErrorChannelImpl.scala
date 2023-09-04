package dev.payment.reminder.impl.error

import dev.payment.reminder.config.KafkaErrorTopicConfig
import dev.payment.reminder.error.{ApplicationError, ErrorChannel, ErrorMessage}
import dev.payment.reminder.impl.event.source.kafka.{KafkaAdmin, KafkaProducer}

import io.circe.Encoder

import zio.{IO, ZIO}

class ErrorChannelImpl(kafkaAdmin: KafkaAdmin, producer: KafkaProducer, topicCfg: KafkaErrorTopicConfig)
    extends ErrorChannel:
  def connect(): IO[ApplicationError, Unit] = kafkaAdmin.createTopicIfNotExists(topicCfg)

  def publishError(error: ErrorMessage): IO[ApplicationError, Unit] =
    for
      jsonString <- ZIO.succeed(Encoder[ErrorMessage].apply(error).spaces2)
      _          <- producer.publish(jsonString, error.id, topicCfg.topic)
    yield ()
