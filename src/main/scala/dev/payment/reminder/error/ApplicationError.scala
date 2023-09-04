package dev.payment.reminder.error

type ApplicationError = ConfigurationError | EventError | KafkaReadError | KafkaCommitError | KafkaProduceError |
  KafkaAdminError

case class ConfigurationError(message: String, cause: Throwable) extends Exception(message, cause)

case class EventError(message: String, cause: Throwable) extends Exception(message, cause)

case class KafkaReadError(message: String, cause: Throwable) extends Exception(message, cause)

case class KafkaCommitError(message: String, cause: Throwable) extends Exception(message, cause)

case class KafkaProduceError(message: String, cause: Throwable) extends Exception(message, cause)

case class KafkaAdminError(message: String, cause: Throwable) extends Exception(message, cause)
