import zio.config.magnolia.descriptor
import zio.config.read
import zio.config.yaml.YamlConfigSource
import zio.test.{assertM, Assertion, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion.equalTo

import java.io.File

import scala.io.Source

object YamlConfigSpec extends DefaultRunnableSpec:
  case class KafkaSecurity(protocol: String)
  case class KafkaClients(bootstrapServers: List[String], timeoutInSeconds: Int, security: KafkaSecurity)
  case class ApplicationConfig(kafkaClients: KafkaClients)
  val applicationConfigDescriptor = descriptor[ApplicationConfig]

  case class KafkaClientsWithoutList(timeoutInSeconds: Int, security: KafkaSecurity)
  case class ApplicationConfigWithoutBootstrapServers(kafkaClients: KafkaClientsWithoutList)
  val applicationConfigWithoutListDescriptor = descriptor[ApplicationConfigWithoutBootstrapServers]

  def spec: ZSpec[Environment, Failure] =
    suite("YamlConfigSourceTest")(
      testM("Reading configuration from only application.yml - Passing test") {
        val applicationYamlSource = YamlConfigSource.fromYamlFile(new File("src/test/resources/application.yml"))
        // """
        //   |kafkaClients:
        //   |    security:
        //   |        protocol: SSL
        //   |    timeoutInSeconds: 300
        //   |    bootstrapServers:
        //   |    - localhost:9092
        //   |    - localhost:9094
        //   |""".stripMargin

        val expected =
          ApplicationConfig(
            KafkaClients(List("localhost:9092", "localhost:9094"), 300, KafkaSecurity("SSL"))
          )
        val result = read(applicationConfigDescriptor from applicationYamlSource)

        assertM(result)(equalTo(expected))
      },
      testM("Reading configuration from application-dev.yml and application.yml - Failing test") {
        val applicationDevYamlSource = YamlConfigSource.fromYamlFile(new File("src/test/resources/application-dev.yml"))
        val applicationYamlSource    = YamlConfigSource.fromYamlFile(new File("src/test/resources/application.yml"))
        val expected =
          ApplicationConfig(
            KafkaClients(List("localhost:9092", "localhost:9094"), 300, KafkaSecurity("PLAINTEXT"))
          )
        val result = read(applicationConfigDescriptor from (applicationDevYamlSource <> applicationYamlSource))

        assertM(result)(equalTo(expected))
      },
      testM("Reading configuration from application-dev.yml and application.yml (without List type) - Passing test") {
        val applicationDevYamlSource = YamlConfigSource.fromYamlFile(new File("src/test/resources/application-dev.yml"))
        val applicationYamlSource = YamlConfigSource.fromYamlFile(new File("src/test/resources/application.yml"))
        val expected =
          ApplicationConfigWithoutBootstrapServers(
            KafkaClientsWithoutList(300, KafkaSecurity("PLAINTEXT"))
          )
        val result = read(applicationConfigWithoutListDescriptor from (applicationDevYamlSource <> applicationYamlSource))

        assertM(result)(equalTo(expected))
      },
      testM("Reading configuration from application yaml using reader - Failing test") {
        val applicationYamlSource =
          YamlConfigSource.fromYamlReader(Source.fromFile("src/test/resources/application.yml").reader)
        val expected =
          ApplicationConfig(
            KafkaClients(List("localhost:9092", "localhost:9094"), 300, KafkaSecurity("SSL"))
          )
        val result = read(applicationConfigDescriptor from applicationYamlSource)

        assertM(result)(equalTo(expected))
      },
      testM("Reading configuration from application yaml using reader (without List type) - Passing test") {
        val applicationYamlSource =
          YamlConfigSource.fromYamlReader(Source.fromFile("src/test/resources/application.yml").reader)
        val expected =
          ApplicationConfigWithoutBootstrapServers(
            KafkaClientsWithoutList(300, KafkaSecurity("SSL"))
          )
        val result = read(applicationConfigWithoutListDescriptor from applicationYamlSource)

        assertM(result)(equalTo(expected))
      }
    )
