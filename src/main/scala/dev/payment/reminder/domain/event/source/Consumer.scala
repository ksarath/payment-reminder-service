package dev.payment.reminder.domain.event.source

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.error.ApplicationError
import dev.payment.reminder.impl.event.source.{ConsumerImpl, KafkaConsumer}

import zio.{ZIO, ZLayer}
import zio.stream.ZStream

trait Consumer:
  def consume(): ZStream[EventPolicy, ApplicationError, Unit]

object Consumer:
  val live: ZLayer[KafkaConsumer with ApplicationConfig, Nothing, Consumer] =
    ZLayer.fromFunction[(KafkaConsumer, ApplicationConfig) => Consumer]((kConsumer, config) =>
      ConsumerImpl(kConsumer, config.paymentEventsConsumer)
    )

  def consumeEvents(): ZIO[Consumer with EventPolicy, Throwable, Unit] =
    for
      _        <- ZIO.logInfo("Starting to consume events")
      consumer <- ZIO.service[Consumer]
      _        <- consumer.consume().runDrain
      _        <- ZIO.logInfo("Consuming of events is completed")
    yield ()
