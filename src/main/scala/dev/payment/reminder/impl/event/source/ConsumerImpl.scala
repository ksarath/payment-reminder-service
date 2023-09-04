package dev.payment.reminder.impl.event.source

import dev.payment.reminder.config.KafkaConsumerConfig
import dev.payment.reminder.domain.event.source.Consumer
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.error.{ApplicationError, ErrorChannel, KafkaCommitError}
import dev.payment.reminder.error.ErrorMessage
import dev.payment.reminder.impl.event.source.kafka.KafkaConsumer
import dev.payment.reminder.util.json.decodeKafkaMessage

import zio.ZIO
import zio.stream.ZStream

class ConsumerImpl(consumer: KafkaConsumer, consumerConfig: KafkaConsumerConfig) extends Consumer:
  override def consume(): ZStream[EventPolicy with ErrorChannel, ApplicationError, Unit] =
    for
      _ <-
        consumer
          .read(consumerConfig.topic)
          .tap(record =>
            (for
              event <- ZIO.fromEither(decodeKafkaMessage(record))
              _     <- EventPolicy.policy(event)
            yield ()).catchAll(e =>
              ZIO.logError(
                s"Error while handling message: (${record.value}) with key: (${record.key}) at offset: (${record.offset}): $e"
              )
              *>
              ErrorChannel.publishError(ErrorMessage(record.key, e.toString, record.value))
            )
          )
          .map(_.offset)
          .aggregateAsync(consumer.offsetBatches)
          .mapZIO(_.commit)
          .mapError(e => KafkaCommitError(s"Error while commiting kafka offset", e))
    yield ()
