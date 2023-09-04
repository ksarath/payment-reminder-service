package dev.payment.reminder.impl.event.source

import dev.payment.reminder.config.KafkaConsumerConfig
import dev.payment.reminder.domain.entities.events.Event
import dev.payment.reminder.domain.event.source.Consumer
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.error.{ApplicationError, KafkaCommitError}
import dev.payment.reminder.util.json.decodeKafkaMessage

import zio.Clock
import zio.ZIO
import zio.ZLayer
import zio.kafka.consumer.CommittableRecord
import zio.kafka.consumer.Offset
import zio.kafka.consumer.OffsetBatch
import zio.kafka.consumer.Subscription
import zio.kafka.serde.Deserializer.fromKafkaDeserializer
import zio.stream.ZStream

class ConsumerImpl(consumer: KafkaConsumer, consumerConfig: KafkaConsumerConfig) extends Consumer:
  override def consume(): ZStream[EventPolicy, ApplicationError, Unit] =
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
            // *>
            //   Publisher.publish(cr.key, cr.value, s"${consumerConfig.consumerGroup}-errors")
            )
          )
          .map(_.offset)
          .aggregateAsync(consumer.offsetBatches)
          .mapZIO(_.commit)
          .mapError(e => KafkaCommitError(s"Error while commiting kafka offset", e))
    yield ()
