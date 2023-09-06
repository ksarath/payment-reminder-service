package dev.payment.reminder.impl.error

import dev.payment.reminder.config.KafkaErrorTopicConfig
import dev.payment.reminder.error.ErrorChannel
import dev.payment.reminder.error.ErrorMessage
import dev.payment.reminder.error.KafkaAdminError
import dev.payment.reminder.error.KafkaProduceError
import dev.payment.reminder.impl.event.source.kafka.{KafkaAdmin, KafkaProducer}
import dev.payment.reminder.mocks.{KafkaAdminMock, KafkaProducerMock}

import io.circe.Encoder

import zio.ZIO
import zio.mock.Expectation.{failure, unit, value}
import zio.test.{assert, assertTrue, assertZIO, ZIOSpecDefault}
import zio.test.Assertion
import zio.test.Assertion.{anything, equalTo, isSubtype, isUnit}
import zio.test.Spec
import zio.test.TestAspect
import zio.test.TestAspect.{timeout, withLiveClock}
import zio.test.TestEnvironment

object ErrorChannelImplTest extends ZIOSpecDefault:
  // val configProvider = ConfigUtil.configProvider("logger.yml")
  val logger =
    zio.Runtime.removeDefaultLoggers
  // >>> zio.Runtime.setConfigProvider(configProvider) >>> zio.logging.consoleLogger()

  override val spec =
    suite("ErrorChannelImpl:")(
      suite("connect")(
        test("should create error topic in kafka") {
          // Set up input & expectations
          val kafkaErrorTopicCfg = KafkaErrorTopicConfig("dev-error-topic", 1, 1)
          val expectKafkaAdminCreateTopicToBeCalledExactlyOnce =
            KafkaAdminMock.createTopicIfNotExistsEffect(equalTo(kafkaErrorTopicCfg), unit).exactly(1)
          val expectKafkaProducerNeverCalled = KafkaProducerMock.empty

          // Call connect action
          val result = (for
            kAdmin    <- ZIO.service[KafkaAdmin]
            kProducer <- ZIO.service[KafkaProducer]
            result    <- ErrorChannelImpl(kAdmin, kProducer, kafkaErrorTopicCfg).connect()
          yield result)
            .provideLayer(
              expectKafkaAdminCreateTopicToBeCalledExactlyOnce ++
                expectKafkaProducerNeverCalled ++
                logger
            )

          // Final result assertion
          assertZIO(result)(isUnit)
        },
        test("should return the error when topic creation in kafka fails") {
          // Set up input & expectations
          val kafkaErrorTopicCfg     = KafkaErrorTopicConfig("dev-error-topic", 1, 1)
          val topicCreationFailedErr = KafkaAdminError("Error while creating topic in kafka", Exception("Boom!!!"))
          val expectKafkaAdminCreateTopicToBeCalledExactlyOnce =
            KafkaAdminMock
              .createTopicIfNotExistsEffect(
                equalTo(kafkaErrorTopicCfg),
                failure(topicCreationFailedErr)
              )
              .exactly(1)
          val expectKafkaProducerNeverCalled = KafkaProducerMock.empty

          // Call connect action
          val result = (for
            kAdmin    <- ZIO.service[KafkaAdmin]
            kProducer <- ZIO.service[KafkaProducer]
            result    <- ErrorChannelImpl(kAdmin, kProducer, kafkaErrorTopicCfg).connect()
          yield result).flip
            .provideLayer(
              expectKafkaAdminCreateTopicToBeCalledExactlyOnce ++
                expectKafkaProducerNeverCalled ++
                logger
            )

          // Final result assertion
          assertZIO(result)(isSubtype[KafkaAdminError](equalTo(topicCreationFailedErr)))
        }
      ),
      suite("publishError")(
        test("should publish error message in kafka") {
          // Set up input & expectations
          val kafkaErrorTopicCfg          = KafkaErrorTopicConfig("dev-error-topic", 1, 1)
          val errorMsg                    = ErrorMessage("id", "error", "message")
          val kafkaMsg                    = Encoder[ErrorMessage].apply(errorMsg).spaces2
          val expectKafkaAdminNeverCalled = KafkaAdminMock.empty
          val expectKafkaProducerPublishToBeCalledOnce =
            KafkaProducerMock.publishEffect(equalTo((kafkaMsg, errorMsg.id, kafkaErrorTopicCfg.topic)), unit)

          // Call connect action
          val result = (for
            kAdmin    <- ZIO.service[KafkaAdmin]
            kProducer <- ZIO.service[KafkaProducer]
            result    <- ErrorChannelImpl(kAdmin, kProducer, kafkaErrorTopicCfg).publishError(errorMsg)
          yield result)
            .provideLayer(
              expectKafkaAdminNeverCalled ++
                expectKafkaProducerPublishToBeCalledOnce ++
                logger
            )

          // Final result assertion
          assertZIO(result)(isUnit)
        },
        test("should return the error when publishing in kafka fails") {
          // Set up input & expectations
          val kafkaErrorTopicCfg          = KafkaErrorTopicConfig("dev-error-topic", 1, 1)
          val errorMsg                    = ErrorMessage("id", "error", "message")
          val kafkaMsg                    = Encoder[ErrorMessage].apply(errorMsg).spaces2
          val kafkaPublishFailErr         = KafkaProduceError("Kafka publish failed", Exception("Boom!!!"))
          val expectKafkaAdminNeverCalled = KafkaAdminMock.empty
          val expectKafkaProducerPublishToBeCalledOnce =
            KafkaProducerMock.publishEffect(
              equalTo((kafkaMsg, errorMsg.id, kafkaErrorTopicCfg.topic)),
              failure(kafkaPublishFailErr)
            )

          // Call connect action
          val result = (for
            kAdmin    <- ZIO.service[KafkaAdmin]
            kProducer <- ZIO.service[KafkaProducer]
            result    <- ErrorChannelImpl(kAdmin, kProducer, kafkaErrorTopicCfg).publishError(errorMsg)
          yield result).flip
            .provideLayer(
              expectKafkaAdminNeverCalled ++
                expectKafkaProducerPublishToBeCalledOnce ++
                logger
            )

          // Final result assertion
          assertZIO(result)(isSubtype[KafkaProduceError](equalTo(kafkaPublishFailErr)))
        }
      )
    ) @@ TestAspect.nonFlaky
