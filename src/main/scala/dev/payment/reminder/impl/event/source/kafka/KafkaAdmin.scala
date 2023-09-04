package dev.payment.reminder.impl.event.source.kafka

import dev.payment.reminder.config.{ApplicationConfig, KafkaErrorTopicConfig}
import dev.payment.reminder.error.KafkaAdminError

import zio.{IO, Scope, ZIO, ZLayer}
import zio.Duration.fromSeconds
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.kafka.admin.AdminClient.NewTopic

trait KafkaAdmin:
  def createTopicIfNotExists(topicCfg: KafkaErrorTopicConfig): IO[KafkaAdminError, Unit]

class KafkaAdminImpl(client: AdminClient) extends KafkaAdmin:
  override def createTopicIfNotExists(topicCfg: KafkaErrorTopicConfig) =
    client
      .listTopics()
      .filterOrElse(_.contains(topicCfg.topic)) {
        client.createTopic(
          NewTopic(topicCfg.topic, topicCfg.partitions, topicCfg.replicationFactor.toShort)
        ) *> client.listTopics()
      }
      .mapError(e => KafkaAdminError(s"Error while creating topic $topicCfg", e))
      .unit

object KafkaAdmin:
  val live: ZLayer[Scope with ApplicationConfig, Throwable, KafkaAdmin] =
    ZLayer {
      for
        config <- ZIO.service[ApplicationConfig]
        adminClient <- AdminClient.make(
                         AdminClientSettings(
                           config.kafkaClients.bootstrapServers,
                           fromSeconds(config.kafkaClients.timeoutInSeconds),
                           Map("security.protocol" -> config.kafkaClients.security.protocol)
                         )
                       )
      yield KafkaAdminImpl(adminClient)
    }

  def createTopicIfNotExists(topicCfg: KafkaErrorTopicConfig): ZIO[KafkaAdmin, Throwable, Unit] =
    for
      service <- ZIO.service[KafkaAdmin]
      _       <- service.createTopicIfNotExists(topicCfg)
    yield ()
