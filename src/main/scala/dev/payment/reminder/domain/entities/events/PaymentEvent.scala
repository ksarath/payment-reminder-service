package dev.payment.reminder.domain.entities.events

type PaymentEvent = PaymentRequestSent | PaymentReceived | PaymentReminderPeriodElapsed | PaymentReminderSent

case class PaymentRequestSent(paymentRequestId: String)

case class PaymentReceived(paymentRequestId: String)

case class PaymentReminderPeriodElapsed(paymentRequestId: String, reminderCount: Int)

case class PaymentReminderSent(paymentRequestId: String, reminderCount: Int)
