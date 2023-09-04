import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.ExecCmd

val dockerDaemonUser = "daemon"
val applicationName  = "payment-reminder-service"
val scala3V          = "3.3.0"
val zioV             = "2.0.15"
val zioConfigV       = "4.0.0-RC16"
val zioKafkaV        = "2.4.2"
val zioLoggingV      = "2.1.13"
val apacheCommonsIOV = "2.8.0"
val circeV           = "0.14.1"
val awsSdkV          = "2.20.115"
val dynosaurV        = "0.6.0"
val logbackV         = "1.4.6"
val zioMockV         = "1.0.0-RC11"
val newrelicAgentV   = "8.5.0"

name := applicationName

version := "0.1"

scalaVersion := scala3V

val mainDependencies = Seq(
  // ZIO
  "dev.zio" %% "zio"         % zioV,
  "dev.zio" %% "zio-streams" % zioV,

  // ZIO Config
  "dev.zio" %% "zio-config"          % zioConfigV,
  "dev.zio" %% "zio-config-magnolia" % zioConfigV,
  "dev.zio" %% "zio-config-yaml"     % zioConfigV,

  // ZIO Logging
  "dev.zio" %% "zio-logging" % zioLoggingV,

  // ZIO Kafka
  "dev.zio" %% "zio-kafka" % zioKafkaV,

  // Circe
  "io.circe" %% "circe-core"    % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser"  % circeV,

  // DynamoDb
  "org.systemfw" %% "dynosaur-core" % dynosaurV,

  // AWS
  "software.amazon.awssdk" % "sts"      % awsSdkV,
  "software.amazon.awssdk" % "ses"      % awsSdkV,
  "software.amazon.awssdk" % "dynamodb" % awsSdkV,
  "software.amazon.awssdk" % "sfn"      % awsSdkV
)

val commonDeps = Seq(
  "commons-io"     % "commons-io"      % apacheCommonsIOV,
  "ch.qos.logback" % "logback-classic" % logbackV
)

val testDeps = Seq(
  "dev.zio" %% "zio-test"          % zioV % "test",
  "dev.zio" %% "zio-test-sbt"      % zioV % "test",
  "dev.zio" %% "zio-test-magnolia" % zioV % "test"
)

libraryDependencies ++= mainDependencies ++ commonDeps ++ testDeps

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

enablePlugins(
  JavaAgent,
  JavaAppPackaging,
  DockerPlugin
)

javaAgents ++= sys.env
  .get("ENVIRONMENT")
  .find(_ == "prod")
  .map(_ => Seq("com.newrelic.agent.java" % "newrelic-agent" % newrelicAgentV % "runtime"))
  .getOrElse(Nil)

Compile / mainClass                    := Some("dev.payment.reminder.Main")
Compile / packageDoc / publishArtifact := false
Docker / packageName                   := applicationName
Docker / daemonUserUid                 := None
Docker / daemonUser                    := dockerDaemonUser
dockerUpdateLatest                     := true
dockerExposedPorts ++= Seq(9000, 9001)
dockerBaseImage := "amazoncorretto:17"
dockerEnvVars ++= sys.env
  .get("ENVIRONMENT")
  .find(_ == "prod")
  .map(_ =>
    Map(
      ("NEW_RELIC_APP_NAME", applicationName),
      ("NEW_RELIC_ACCOUNT_ID", ""),
      ("NEW_RELIC_INSIGHTS_KEY", ""),
      ("NEW_RELIC_LICENSE_KEY", ""),
      ("NEW_RELIC_LOG_FILE_NAME", "STDOUT"),
      (
        "NEW_RELIC_SYNC_STARTUP",
        "true"
      ), // Connect the New Relic collector immediately upon app startup, so that we definitely connect before service exits
      (
        "NEW_RELIC_SEND_DATA_ON_EXIT",
        "true"
      ), // Enable delayed JVM shutdown to give the agent a chance to send latest metric data to New Relic before JVM shutdown.
      (
        "NEW_RELIC_SEND_DATA_ON_EXIT_THRESHOLD",
        "0"
      ) // Apply SEND_DATA_ON_EXIT immediately (defaults to 60 seconds), so we send data on exit even if service runs for less than 60 seconds.
    )
  )
  .getOrElse(Map.empty)
Docker / dockerCommands := sys.env
  .get("ENVIRONMENT")
  .find(_ == "prod")
  .map(_ => 
    {
      val commands = dockerCommands.value
      val index = commands.indexWhere {
        case Cmd("USER", args) => args == dockerDaemonUser
        case _                 => false
      }
      commands.patch(
        index,
        Seq(
          ExecCmd(
            "RUN",
            "curl",
            "-o",
            "/opt/docker/newrelic-agent/newrelic.yml",
            "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.yml"
          )
        ),
        0
      )
    }
  )
  .getOrElse(dockerCommands.value)
