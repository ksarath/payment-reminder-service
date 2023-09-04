package dev.payment.reminder.domain.entities.events

import java.time.Instant

final case class Event(happenedOn: Instant, event: PaymentEvent)

object Event:
  def apply(event: PaymentEvent): Event =
    Event(Instant.now, event)
