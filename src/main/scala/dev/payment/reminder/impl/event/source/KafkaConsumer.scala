package dev.payment.reminder.impl.event.source

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.error.KafkaReadError

import zio.{Schedule, ZIO, ZLayer}
import zio.Duration.fromMillis
import zio.Scope
import zio.durationInt
import zio.kafka.consumer.{CommittableRecord, ConsumerSettings, Offset, OffsetBatch}
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.serde.Deserializer.fromKafkaDeserializer
import zio.kafka.serde.Serde
import zio.stream.ZSink
import zio.stream.ZStream

trait KafkaConsumer:
  def read(topic: String): ZStream[Any, KafkaReadError, CommittableRecord[String, String]]
  val offsetBatches: ZSink[Any, Nothing, Offset, Nothing, OffsetBatch]

class KafkaConsumerImpl(consumer: Consumer) extends KafkaConsumer:
  def read(topic: String) =
    consumer
      .plainStream(Subscription.topics(topic), Serde.string, Serde.string)
      .mapError(e => KafkaReadError(s"Error while reading kafka topic $topic", e))

  val offsetBatches = Consumer.offsetBatches

object KafkaConsumer:
  val live: ZLayer[Scope with ApplicationConfig, Throwable, KafkaConsumer] =
    ZLayer {
      for
        config <- ZIO.service[ApplicationConfig]
        consumer <- Consumer.make(
                      ConsumerSettings(
                        bootstrapServers = config.kafkaClients.bootstrapServers,
                        properties = Map("security.protocol" -> config.kafkaClients.security.protocol),
                        closeTimeout = fromMillis(1000),
                        pollTimeout = fromMillis(5000),
                        offsetRetrieval = OffsetRetrieval.Auto(AutoOffsetStrategy.Latest)
                      ).withGroupId(config.paymentEventsConsumer.consumerGroup)
                    )
      yield KafkaConsumerImpl(consumer)
    }

  def read(topic: String): ZStream[KafkaConsumer, KafkaReadError, CommittableRecord[String, String]] =
    for
      service <- ZStream.fromZIO(ZIO.service[KafkaConsumer])
      record  <- service.read(topic)
    yield record

  val offsetBatches: ZIO[KafkaConsumer, Nothing, ZSink[Any, Nothing, Offset, Nothing, OffsetBatch]] =
    ZIO.service[KafkaConsumer].map(_.offsetBatches)
