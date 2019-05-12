package services

import java.sql.{Date, Timestamp}
import java.time.LocalDateTime

import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}


object FormatService {

  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    def write(date: Date) = JsString(date.toString)

    def read(value: JsValue): Date = value match {
      case JsString(dateStr) => Date.valueOf(dateStr)
      case _ => throw new DeserializationException("Date expected")
    }
  }

  implicit object TimestampJsonFormat extends RootJsonFormat[Timestamp] {
    def write(ts: Timestamp) = JsString(ts.toString)

    def read(value: JsValue): Timestamp = value match {
      case JsString(tsStr) => Timestamp.valueOf(tsStr)
      case _ => throw new DeserializationException("Date expected")
    }
  }

}
