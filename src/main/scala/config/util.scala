package config

import org.apache.commons.io.IOUtils

import zio.Has
import zio.ZIO
import zio.ZLayer
import zio.ZManaged
import zio.config.ConfigDescriptor
import zio.config.ConfigDescriptor.*
import zio.config.ConfigSource
import zio.config.ReadError
import zio.config.ZConfig.fromSystemEnv
import zio.config.magnolia.descriptor
import zio.config.read
import zio.config.yaml.YamlConfigSource.fromYamlFile
import zio.system.System

import java.io.{File, FileOutputStream}
import java.io.Reader

import scala.io.Source
import scala.util.Try

lazy val configurationLayer = System.live >>> environmentLayer >>> applicationConfigLayer("application.yml")

lazy val environmentLayer: ZLayer[System, ReadError[String], Has[Option[Environment]]] =
  fromSystemEnv(environmentDescriptor, None, Some(','))

def applicationConfigLayer(
  yamlResourceFilePath: String,
  appConfigDescriptor: ConfigDescriptor[ApplicationConfig] = applicationConfigDescriptor
): ZLayer[Has[Option[Environment]], Throwable, Has[ApplicationConfig]] =
  (for
    optionalEnv <- ZIO.service[Option[Environment]]
    config      <- configSource(optionalEnv, yamlResourceFilePath).use(source => read(appConfigDescriptor from source))
  yield config).toLayer

private def configSource(
  env: Option[Environment],
  yamlResourceFilePath: String
): ZManaged[Any, Throwable, ConfigSource] =
  val systemEnvSource = ConfigSource.fromSystemEnv(None, Some(','))

  env match
    case None => (
      for file <- ZManaged.make(acquire(yamlResourceFilePath))(release)
      yield fromYamlFile(file) // systemEnvSource <> BUG: https://github.com/zio/zio-config/issues/783
    )

    case Some(env) => (
      for
        file                  <- ZManaged.make(acquire(yamlResourceFilePath))(release)
        (fileName, fileExt)    = yamlResourceFilePath splitAt yamlResourceFilePath.lastIndexOf(".")
        envSpecificFilePath    = s"$fileName-${env.name()}$fileExt"
        readerEnvSpecificFile <- ZManaged.make(acquire(envSpecificFilePath))(release)
      yield fromYamlFile(readerEnvSpecificFile)
      // systemEnvSource <> <> fromYamlFile(file) BUG: https://github.com/zio/zio-config/issues/783
    )

private def acquire(yamlResourceFilePath: String) = ZIO.effect(readResourceFile(yamlResourceFilePath))
private def release(file: File) = ZIO.effectTotal {
  file.delete()
}

private def readResourceFile(file: String): File =
  val inputStream = this.getClass.getClassLoader.getResourceAsStream(file)
  val tempFile    = File.createTempFile("payment_reminder", "application_configuration")
  tempFile.deleteOnExit()
  IOUtils.copy(inputStream, new FileOutputStream(tempFile))

  tempFile

/**
 * Environment configuration descriptors
 */

private lazy val environmentDescriptor: ConfigDescriptor[Option[Environment]] =
  (string("ENVIRONMENT").default("dev"))(
    v => Try(Environment.valueOf(v.toUpperCase)).toOption,
    _.map(_.name())
  )

/**
 * Application configuration descriptors
 */

private lazy val kafkaClientsConfigDescriptor: ConfigDescriptor[KafkaClientsConfig] =
  descriptor[KafkaClientsConfig]

private lazy val paymentEventsKafkaConsumerConfigDescriptor: ConfigDescriptor[KafkaConsumerConfig] =
  descriptor[KafkaConsumerConfig]

private lazy val applicationConfigDescriptor: ConfigDescriptor[ApplicationConfig] =
  (
    nested("paymentReminder")(
      nested("eventSource")(
        nested("kafka")(
          nested("clients")(kafkaClientsConfigDescriptor)
        )
      )
    ) zip
      nested("paymentReminder")(
        nested("eventSource")(
          nested("kafka")(nested("consumers")(nested("paymentEvents")(paymentEventsKafkaConsumerConfigDescriptor)))
        )
      )
  ).to[ApplicationConfig]
