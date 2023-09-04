package dev.payment.reminder.impl.event.source.kafka

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.error.KafkaProduceError

import org.apache.kafka.clients.producer.ProducerRecord

import zio.{durationInt, Scope}
import zio.{IO, Schedule, ZIO, ZLayer}
import zio.Duration.fromSeconds
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde

trait KafkaProducer:
  def publish(message: String, partitionKey: String, topic: String): IO[KafkaProduceError, Unit]

class KafkaProducerImpl(producer: Producer) extends KafkaProducer:
  def publish(message: String, partitionKey: String, topic: String) =
    producer
      .produce(
        new ProducerRecord(topic, partitionKey, message),
        Serde.string,
        Serde.string
      )
      .mapError(e => KafkaProduceError(s"Error occured while publishing message to kafka", e))
      .unit

object KafkaProducer:
  val live: ZLayer[Scope with ApplicationConfig, Throwable, KafkaProducer] =
    ZLayer {
      for
        config <- ZIO.service[ApplicationConfig]
        producer <- Producer.make(
                      ProducerSettings(
                        config.kafkaClients.bootstrapServers,
                        fromSeconds(config.kafkaClients.timeoutInSeconds),
                        4096,
                        Map("security.protocol" -> config.kafkaClients.security.protocol)
                      )
                    )
      yield KafkaProducerImpl(producer)
    }

  def publish(message: String, partitionKey: String, topic: String): ZIO[KafkaProducer, KafkaProduceError, Unit] =
    for
      service <- ZIO.service[KafkaProducer]
      _       <- service.publish(message, partitionKey, topic)
    yield ()
