package dev.payment.reminder.domain.policies

import dev.payment.reminder.domain.entities.events.Event
import dev.payment.reminder.error.ApplicationError
import dev.payment.reminder.impl.policies.EventPolicyImpl

import zio.{ZIO, ZLayer}

trait EventPolicy:
  def policy(event: Event): ZIO[Any, ApplicationError, Unit]

object EventPolicy:
  val live: ZLayer[Any, Nothing, EventPolicy] =
    ZLayer.succeed(EventPolicyImpl())

  def policy(event: Event): ZIO[EventPolicy, ApplicationError, Unit] =
    for
      _           <- ZIO.logInfo(s"Starting to process event: $event")
      eventPolicy <- ZIO.service[EventPolicy]
      _           <- eventPolicy.policy(event)
      _           <- ZIO.logInfo(s"Completed processing event: $event")
    yield ()
