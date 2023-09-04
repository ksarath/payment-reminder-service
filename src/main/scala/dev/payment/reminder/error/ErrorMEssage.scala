package dev.payment.reminder.error

import io.circe.*
import io.circe.generic.semiauto.*

case class ErrorMessage(id: String, error: String, message: String)

object ErrorMessage:
  implicit val errorMessageDecoder: Decoder[ErrorMessage] = deriveDecoder[ErrorMessage]
  implicit val errorMessageEncoder: Encoder[ErrorMessage] = deriveEncoder[ErrorMessage]
