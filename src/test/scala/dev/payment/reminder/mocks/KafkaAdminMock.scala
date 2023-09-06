package dev.payment.reminder.mocks

import dev.payment.reminder.config.KafkaErrorTopicConfig
import dev.payment.reminder.error.KafkaAdminError
import dev.payment.reminder.impl.event.source.kafka.KafkaAdmin

import zio.{IO, ZIO, ZLayer}
import zio.mock.*

object KafkaAdminMock extends Mock[KafkaAdmin]:
  object createTopicIfNotExistsEffect extends Effect[KafkaErrorTopicConfig, KafkaAdminError, Unit]

  val compose = ZLayer {
    for proxy <- ZIO.service[Proxy]
    yield new KafkaAdmin:
      def createTopicIfNotExists(topicCfg: KafkaErrorTopicConfig) =
        proxy(createTopicIfNotExistsEffect, topicCfg)
  }
