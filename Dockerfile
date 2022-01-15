FROM adoptopenjdk/openjdk11

COPY /target/scala-3.1.0/payment-reminder-assembly.jar /application/payment-reminder.jar

CMD ["java", "-jar", "/application/payment-reminder.jar"]
