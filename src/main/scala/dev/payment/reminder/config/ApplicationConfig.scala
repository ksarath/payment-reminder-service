package dev.payment.reminder.config

import dev.payment.reminder.error.ConfigurationError
import dev.payment.reminder.util.config.{appEnvConf, applicationConfig, getConfig}

import zio.{ZIO, ZLayer}

case class ApplicationConfig(kafkaClients: KafkaClientsConfig, paymentEventsConsumer: KafkaConsumerConfig)

case class KafkaConsumerConfig(topic: String, consumerGroup: String)

case class KafkaClientsConfig(
  bootstrapServers: List[String],
  timeoutInSeconds: Int,
  security: KafkaClientsSecurityConfig
)

case class KafkaClientsSecurityConfig(protocol: String)

object ApplicationConfig:
  private def liveConfiguration(
    file: String = "application.yml"
  ) = ZLayer.fromZIO(for
    env    <- ZIO.service[Option[Environment]]
    config <- getConfig(file, applicationConfig, env)
  yield config)

  val live: ZLayer[Any, ConfigurationError, ApplicationConfig] = ZLayer.fromZIO(appEnvConf) >>> liveConfiguration()
