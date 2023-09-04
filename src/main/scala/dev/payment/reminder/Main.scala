package dev.payment.reminder

import dev.payment.reminder.config.ApplicationConfig
import dev.payment.reminder.domain.event.source.Consumer
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.impl.event.source.KafkaConsumer
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
      KafkaConsumer.live,
      ApplicationConfig.live
      // AdminService.live,
      // MessageHandler.live,
      // MessagePublisher.live,
      // MessageConsumer.live,
      // TransferReadModelClient.live,
      // ManuscriptReadModelClient.live,
      // SubmissionFilesReadModelClient.live
    )

  // def createDeadLetterTopic: ZIO[ApplicationConfig with AdminService, Throwable, Unit] =
  //   for
  //     config <- ZIO.service[AppConfig]
  //     _      <- AdminService.createTopicIfNotExists(KafkaTopic(s"${config.consumerConfig.consumerGroup}-deadletter", 1, 1))
  //   yield ()

  val program =
    for
      _ <- ZIO.logInfo("Started payment reminder application")
      // _ <- createDeadLetterTopic
      _ <- Consumer.consumeEvents()
      _ <- ZIO.logInfo(s"Shutting down payment reminder application")
    yield ()