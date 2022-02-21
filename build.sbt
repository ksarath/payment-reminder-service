import sbt.Keys.parallelExecution

name := "payment-reminder-service"

ThisBuild / scalaVersion := Version.scala3

ThisBuild / Compile / mainClass        := Some("Main")
ThisBuild / run / mainClass            := Some("Main")
ThisBuild / packageBin / mainClass     := Some("Main")
ThisBuild / assembly / assemblyJarName := s"${name.value}-assembly.jar"

ThisBuild / dynverSeparator  := "-"
ThisBuild / dynverVTagPrefix := false

enablePlugins(CucumberPlugin, DockerPlugin)

IntegrationTest / parallelExecution := false
CucumberPlugin.glues                := List("com.waioeka.sbt", "steps")

// workaround to accept the integration test classpath to cucumber plugin
CucumberPlugin.javaOptions := List(
  "-classpath",
  ((fullClasspath in IntegrationTest) map { cp => cp.toList.map(_.data.toPath) }).value mkString ":"
)

docker / dockerfile := {
  val jarFile: File = (Compile / packageBin / sbt.Keys.`package`).value
  val classpath     = (Compile / managedClasspath).value
  val mainclass     = (Compile / packageBin / mainClass).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget     = s"/application/${name.value}.jar"

  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/application/" + _.getName).mkString(":") + ":" + jarTarget

  new Dockerfile {
    // Base image
    from("adoptopenjdk/openjdk11")
    // Add all files on the classpath
    add(classpath.files, "/application/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

ThisBuild / assemblyMergeStrategy := {
  case "module-info.class"                     => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case PathList(ps @ _*)
      if Set(
        "codegen.config",
        "service-2.json",
        "waiters-2.json",
        "customization.config",
        "examples-1.json",
        "paginators-1.json"
      ).contains(ps.last) =>
    MergeStrategy.discard
  case x => MergeStrategy.defaultMergeStrategy(x)
}

val mainDependencies = Seq(
  // ZIO
  "dev.zio" %% "zio"         % Version.zio,
  "dev.zio" %% "zio-streams" % Version.zio,

  // ZIO Config
  "dev.zio" %% "zio-config"          % Version.zioConfig,
  "dev.zio" %% "zio-config-magnolia" % Version.zioConfig,
  "dev.zio" %% "zio-config-yaml"     % Version.zioConfig,

  // ZIO Logging
  "dev.zio" %% "zio-logging" % Version.zioLogging,

  // ZIO Kafka
  "dev.zio" %% "zio-kafka" % Version.zioKafka,

  // Circe
  "io.circe" %% "circe-core"    % Version.circeV,
  "io.circe" %% "circe-generic" % Version.circeV,
  "io.circe" %% "circe-parser"  % Version.circeV,

  // DynamoDb
  "org.systemfw" %% "dynosaur-core" % Version.dynosaur,

  // AWS
  "software.amazon.awssdk" % "sts"      % Version.awsSdk,
  "software.amazon.awssdk" % "ses"      % Version.awsSdk,
  "software.amazon.awssdk" % "dynamodb" % Version.awsSdk,
  "software.amazon.awssdk" % "sfn"      % Version.awsSdk
)

val commonDeps = Seq(
  "commons-io"     % "commons-io"      % Version.apacheCommonsIO,
  "ch.qos.logback" % "logback-classic" % Version.logback
)

val testDeps = Seq(
  "dev.zio" %% "zio-test"          % Version.zio % "test",
  "dev.zio" %% "zio-test-sbt"      % Version.zio % "test",
  "dev.zio" %% "zio-test-magnolia" % Version.zio % "test"
)

val itTestDeps = Seq(
  "io.cucumber"                %% "cucumber-scala"       % Version.cucumberScala   % "it",
  "io.cucumber"                 % "cucumber-junit"       % Version.cucumberJunit   % "it",
  "org.testcontainers"          % "testcontainers"       % Version.testcontainersV % "it",
  "com.dimafeng"               %% "testcontainers-scala" % "0.39.12"               % "it",
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.9.4"                 % "it"
)

libraryDependencies ++= mainDependencies ++ commonDeps ++ testDeps ++ itTestDeps

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val paymentReminderService =
  project.in(file(".")).configs(IntegrationTest).settings(Defaults.itSettings)
//settings(inConfig(IntegrationTest)(JupiterPlugin.scopedSettings))
