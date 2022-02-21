# payment-reminder-service

This is a sample application showing how to employ functional programming to provide effective solutions to problems.

This application sends a reminder to customers for unpaid invoices. It works in the following way
- It listens to `InvoiceSent` and `PaymentReceived` domain events
- It generates and listens to `PaymentReminderPeriodElapsed` domain event
- In case the payment is not yet received by the time it receives `PaymentReminderPeriodElapsed`, it generates `PaymentReminderSent` domain event
- It listens to `PaymentReminderSent` events and sends a reminder

Some technical details:
  - Uses ZIO echo system
  - Uses kafka for event persistence 
  - Uses Quartz library for scheduling
  - Uses AWS dynamo db for read model projection
  - Uses AWS SES for sending reminder (email)

## Running the application

- **Format sources** `sbt scalafmt` 
- **Compile** `sbt clean compile` or `bloop clean && bloop compile paymentReminderService`
- **Run unit tests** `sbt test`
- **Package (jar)** `sbt package` 
- **Create docker image** `sbt docker`
- **Run** `sbt run` or `bloop run paymentReminderService`
- **Run in a docker container** `sbt docker && docker-compose up`
- **Run integration tests** `sbt cucumber`