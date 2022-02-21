package config

case class ApplicationConfig(kafkaClients: KafkaClientsConfig, paymentEventsConsumer: KafkaConsumerConfig)

case class KafkaConsumerConfig(topic: String, consumerGroup: String)

case class KafkaClientsConfig(
  bootstrapServers: List[String],
  timeoutInSeconds: Int,
  security: KafkaClientsSecurityConfig
)

case class KafkaClientsSecurityConfig(protocol: String)
