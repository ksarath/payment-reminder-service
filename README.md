# payment-reminder-service

This is a sample application showing how to employ functional programming to provide effective solutions to problems.

This application sends a reminder to customers for unpaid invoices. It works in the following way
- It listens to `PaymentRequestSent` and `PaymentReceived` domain events
- It generates and listens to `PaymentReminderPeriodElapsed` domain event
- In case the payment is not yet received by the time it receives `PaymentReminderPeriodElapsed`, it generates `PaymentReminderSent` domain event
- It listens to `PaymentReminderSent` events and sends reminders

Some technical details:
  - Uses ZIO echo system
  - Uses kafka for event persistence 
  - Uses Quartz library for scheduling
  - Uses AWS dynamo db for read model projection
  - Uses AWS SES for sending reminder (email)

## Running the application

- **Format sources** `sbt scalafmt` 
- **Compile** `sbt clean compile` or `bloop clean && bloop compile payment-reminder-service`
- **Run unit tests** `sbt test` or `bloop test payment-reminder-service`
- **Package (jar)** `sbt package` 
- **Create Dockerfile** `sbt "Docker / stage"`
- **Create docker image** `sbt "Docker / publishLocal"`
- **Run** `sbt run` or `bloop run payment-reminder-service`
- **Run in a docker container** `sbt "Docker / publishLocal" && docker-compose -f docker-compose-local.yml up`

### Producing kafka messages to local kafka

- Download Kafka from `https://kafka.apache.org/downloads`
- Unzip the downloaded file, and cd to the unzipped folder
- **Create a local kafka producer configuration** `echo "security.protocol=PLAINTEXT" > kafka_local.conf`
- **Run kafka console producer** `bin/kafka-console-producer.sh --topic payment-events-dev --bootstrap-server localhost:9092 --producer.config kafka_local.conf --property parse.key=true --property key.separator=:`
- **Produce a (sample) message** `payment-request-123:{"happenedOn": "2023-02-21T11:57:15.216077Z", "name": "PaymentRequestSent", "event": {"paymentRequestId": "payment-req-123"}}`

## Licence
[License](LICENSE)