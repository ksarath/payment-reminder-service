package dev.payment.reminder.error

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.impl.error.ErrorChannelImpl
import dev.payment.reminder.impl.event.source.kafka.{KafkaAdmin, KafkaProducer}

import zio.{IO, ZIO, ZLayer}

trait ErrorChannel:
  def connect(): IO[ApplicationError, Unit]
  def publishError(error: ErrorMessage): IO[ApplicationError, Unit]

object ErrorChannel:
  lazy val live: ZLayer[KafkaAdmin with KafkaProducer with ApplicationConfig, Nothing, ErrorChannel] =
    ZLayer.fromFunction[(KafkaAdmin, KafkaProducer, ApplicationConfig) => ErrorChannel]((admin, producer, config) =>
      ErrorChannelImpl(admin, producer, config.paymentEventsError)
    )

  def connect(): ZIO[ErrorChannel, ApplicationError, Unit] =
    for
      service <- ZIO.service[ErrorChannel]
      _       <- service.connect()
    yield ()

  def publishError(error: ErrorMessage): ZIO[ErrorChannel, ApplicationError, Unit] =
    for
      service <- ZIO.service[ErrorChannel]
      _       <- service.publishError(error)
    yield ()
