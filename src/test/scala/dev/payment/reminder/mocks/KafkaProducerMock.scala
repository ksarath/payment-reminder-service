package dev.payment.reminder.mocks

import dev.payment.reminder.config.KafkaErrorTopicConfig
import dev.payment.reminder.error.KafkaProduceError
import dev.payment.reminder.impl.event.source.kafka.KafkaProducer

import zio.{IO, ZIO, ZLayer}
import zio.mock.*

object KafkaProducerMock extends Mock[KafkaProducer]:
  object publishEffect extends Effect[(String, String, String), KafkaProduceError, Unit]

  val compose = ZLayer {
    for proxy <- ZIO.service[Proxy]
    yield new KafkaProducer:
      def publish(message: String, partitionKey: String, topic: String) =
        proxy(publishEffect, message, partitionKey, topic)
  }
