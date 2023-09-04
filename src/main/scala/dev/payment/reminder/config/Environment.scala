package dev.payment.reminder.config

enum Environment:
  case DEV, IT, TEST, PROD

object Environment:
  extension (e: Environment) def name(): String = e.toString.toLowerCase
