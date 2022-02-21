import zio.{App, ExitCode, Has, UIO, URIO, ZEnv, ZIO}
import zio.logging.{log, LogAnnotation, Logging}
import zio.system.System

import config.ApplicationConfig
import config.configurationLayer

object Main extends App:
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program
      .tapError(e => log.throwable("Execution failed with an error :( ", e))
      .provideSomeLayer[ZEnv](configurationLayer ++ Logging.console())
      .exitCode

  val program: ZIO[Has[ApplicationConfig] & Logging, Throwable, Unit] =
    for
      _      <- log.info("Started payment reminder application")
      config <- ZIO.service[ApplicationConfig]
      _      <- log.info(s"Application configuration: $config")
    yield ()
