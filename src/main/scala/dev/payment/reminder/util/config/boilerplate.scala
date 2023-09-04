package dev.payment.reminder.util.config

import dev.payment.reminder.config.*
import dev.payment.reminder.error.ConfigurationError

import org.apache.commons.io.IOUtils

import zio.*
import zio.Config.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.yaml.YamlConfigProvider

import java.io.{File, FileOutputStream}

import scala.util.Try

val appEnvConf: IO[ConfigurationError, Option[Environment]] = (
  for
    envS <- ConfigProvider.defaultProvider.load(string("ENVIRONMENT").optional)
    env   = envS.flatMap(e => Try(Environment.valueOf(e.toUpperCase)).toOption)
  yield env
).mapError(e => ConfigurationError(s"Error while reading ENVIRONMENT", e))

def getConfig[A](file: String, config: Config[A], environment: Option[Environment] = None): IO[ConfigurationError, A] =
  configProvider(file, environment)
    .load(config)
    .mapError(e => ConfigurationError(s"Error while reading application configuration", e))

def configProvider(file: String, environment: Option[Environment] = None): ConfigProvider =
  val systemEnvSource             = ConfigProvider.defaultProvider
  val commonApplicationConfSource = YamlConfigProvider.fromYamlFile(readResourceFile(file))

  environment match
    case None =>
      systemEnvSource orElse commonApplicationConfSource
    case Some(env) =>
      val (fileName, fileExt) = file splitAt file.lastIndexOf(".")
      val envSpecificSource =
        YamlConfigProvider.fromYamlFile(readResourceFile(s"$fileName-${env.name()}$fileExt"))
      systemEnvSource orElse envSpecificSource orElse commonApplicationConfSource

private def readResourceFile(file: String): File =
  val inputStream = this.getClass.getClassLoader.getResourceAsStream(file)
  val tempFile    = File.createTempFile("authorhub", "em_events_producer_config")
  tempFile.deleteOnExit()
  IOUtils.copy(inputStream, new FileOutputStream(tempFile))

  tempFile

private val kafkaConsumerConfig = deriveConfig[KafkaConsumerConfig]
private val kafkaClientsConfig  = deriveConfig[KafkaClientsConfig]
val applicationConfig =
  (
    kafkaClientsConfig.nested("clients").nested("kafka").nested("eventSource").nested("paymentReminder") zip
      kafkaConsumerConfig
        .nested("paymentEvents")
        .nested("consumers")
        .nested("kafka")
        .nested("eventSource")
        .nested("paymentReminder")
  ).to[ApplicationConfig]
