package dev.payment.reminder

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.domain.event.source.Consumer
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.error.ErrorChannel
import dev.payment.reminder.impl.event.source.kafka.*
import dev.payment.reminder.util.config.configProvider

import zio.Cause
import zio.Runtime
import zio.Scope
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer
import zio.logging.consoleLogger

object Main extends ZIOAppDefault:

  override val bootstrap =
    Runtime.removeDefaultLoggers >>>
      Runtime.setConfigProvider(configProvider("logger.yml")) >>>
      consoleLogger()

  def run = program
    .tapError(e => ZIO.logCause("Exiting: Application failed with an error :( ", Cause.die(e)))
    .provide(
      Scope.default,
      Consumer.live,
      EventPolicy.live,
      ErrorChannel.live,
      KafkaAdmin.live,
      KafkaConsumer.live,
      KafkaProducer.live,
      ApplicationConfig.live
    )

  val program =
    for
      _ <- ZIO.logInfo("Started payment reminder application")
      _ <- ErrorChannel.connect()
      _ <- Consumer.consumeEvents()
      _ <- ZIO.logInfo(s"Shutting down payment reminder application")
    yield ()
