package dev.payment.reminder.domain.entities.events

type PaymentEvent = PaymentRequestSent | PaymentReceived | PaymentReminderPeriodElapsed | PaymentReminderSent

case class PaymentRequestSent(paymentRequestId: String)

case class PaymentReceived(paymentRequestId: String)

case class PaymentReminderPeriodElapsed(paymentRequestId: String, reminderCount: Int)

case class PaymentReminderSent(paymentRequestId: String, reminderCount: Int)

extension (e: PaymentEvent)
  def identifier: String = e match
    case e: PaymentRequestSent           => e.paymentRequestId
    case e: PaymentReceived              => e.paymentRequestId
    case e: PaymentReminderPeriodElapsed => e.paymentRequestId
    case e: PaymentReminderSent          => e.paymentRequestId
