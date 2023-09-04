package dev.payment.reminder.domain.event

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.domain.entities.events.Event
import dev.payment.reminder.error.ApplicationError
import dev.payment.reminder.impl.event.EventPublisherImpl
import dev.payment.reminder.impl.event.source.kafka.KafkaProducer

import zio.{IO, ZIO, ZLayer}

trait EventPublisher:
  def publish(evt: Event): IO[ApplicationError, Unit]

object EventPublisher:
  lazy val live: ZLayer[KafkaProducer with ApplicationConfig, Nothing, EventPublisher] =
    ZLayer.fromFunction[(KafkaProducer, ApplicationConfig) => EventPublisher]((producer, config) =>
      EventPublisherImpl(producer, config.paymentEventsProducer.topic)
    )

  def publish(evt: Event): ZIO[EventPublisher, ApplicationError, Unit] =
    for
      service <- ZIO.service[EventPublisher]
      _       <- service.publish(evt)
    yield ()
