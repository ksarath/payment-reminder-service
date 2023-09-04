package dev.payment.reminder.impl.policies

import dev.payment.reminder.domain.entities.events.*
import dev.payment.reminder.domain.policies.EventPolicy
import dev.payment.reminder.error.ApplicationError

import zio.ZIO
import zio.ZLayer

class EventPolicyImpl() extends EventPolicy:
  def policy(event: Event): ZIO[Any, ApplicationError, Unit] =
    event.event match
      case e: PaymentRequestSent =>
        ZIO.logInfo(s"Received ${e.getClass.getSimpleName} event: $e")
      case e: PaymentReceived =>
        ZIO.logInfo(s"Received ${e.getClass.getSimpleName} event: $e")
      case e: PaymentReminderPeriodElapsed =>
        ZIO.logInfo(s"Received ${e.getClass.getSimpleName} event: $e")
      case e: PaymentReminderSent =>
        ZIO.logInfo(s"Received ${e.getClass.getSimpleName} event: $e")
