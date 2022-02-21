package config

enum Environment:
  case DEV, TEST, PROD

extension (e: Environment) def name(): String = e.toString.toLowerCase
