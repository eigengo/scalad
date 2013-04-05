package org.eigengo.scalad.mongo.sprayjson

import spray.json._
import java.util.{UUID, Date}
import java.net.URI
import org.eigengo.scalad.mongo.{UuidChecker, IsoDateChecker}

/** Convenient implicit conversions */
object BigNumberMarshalling {

  import language.implicitConversions

  implicit def StringBigDecimalToBigDecimal(value: StringBigDecimal) = value.value

  implicit def StringBigIntBigDecimal(value: StringBigInt) = value.value

  implicit def StringToStringBigDecimal(value: String) = StringBigDecimal(value)

  implicit def StringToStringBigInt(value: String) = StringBigInt(value)

  implicit def IntToStringBigDecimal(value: Int) = StringBigDecimal(BigDecimal(value))

  implicit def IntToStringBigInt(value: Int) = StringBigInt(BigInt(value))

  implicit def BigDecimalToStringBigDecimal(value: BigDecimal) = StringBigDecimal(value)

  implicit def BigIntToStringBigInt(value: BigInt) = StringBigInt(value)
}

/** Alternative to [[spray.json.BasicFormats]] `JsNumber` marshalling. */
trait BigNumberMarshalling {

  implicit object StringBigDecimalJsonFormat extends RootJsonFormat[StringBigDecimal] {
    def write(obj: StringBigDecimal) = JsString(obj.value.toString())

    def read(json: JsValue) = json match {
      case JsString(value) => StringBigDecimal(value)
      case _ => deserializationError("Expected String for StringBigDecimal")
    }
  }

  implicit object StringBigIntJsonFormat extends RootJsonFormat[StringBigInt] {

    def write(obj: StringBigInt) = JsString(obj.value.toString())

    def read(json: JsValue) = json match {
      case JsString(value) => StringBigInt(value)
      case _ => deserializationError("Expected String for StringBigInt")
    }
  }

}

trait DateMarshalling {

  implicit object DateJsonFormat extends BsonMarshalling[Date] with IsoDateChecker {

    override val key = "$date"

    override def writeString(obj: Date) = dateToIsoString(obj)

    override def readString(value: String) = parseIsoDateString(value) match {
      case None => deserializationError("Expected ISO Date format, got %s" format (value))
      case Some(date) => date
    }
  }

}

/** [[scala.math.BigDecimal]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
final case class StringBigDecimal(value: BigDecimal)

object StringBigDecimal {
  def apply(value: String) = new StringBigDecimal(BigDecimal(value))
}

/** [[scala.math.BigInt]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
final case class StringBigInt(value: BigInt)

object StringBigInt {
  def apply(value: String) = new StringBigInt(BigInt(value))
}

/** Allows special types to be marshalled into a meta JSON language
  * which allows ScalaD Mongo serialisation to convert into the correct
  * BSON representation for database persistence.
  */
trait BsonMarshalling[T] extends RootJsonFormat[T] {

  val key: String

  def writeString(obj: T): String

  def readString(value: String): T

  def write(obj: T) = JsObject(key -> JsString(writeString(obj)))

  def read(json: JsValue) = json match {
    case JsObject(map) => map.get(key) match {
      case Some(JsString(text)) => readString(text)
      case x => deserializationError("Expected %s, got %s" format(key, x))
    }
    case x => deserializationError("Expected JsObject, got %s" format (x))
  }

}

trait UuidMarshalling {

  implicit object UuidJsonFormat extends BsonMarshalling[UUID] with UuidChecker {

    override val key = "$uuid"

    override def writeString(obj: UUID) = obj.toString

    override def readString(value: String) = parseUuidString(value) match {
      case None => deserializationError("Expected UUID format, got %s" format (value))
      case Some(uuid) => uuid
    }
  }

}


trait UriMarshalling {

  implicit protected object UriJsonFormat extends RootJsonFormat[URI] {
    def write(x: URI) = JsString(x toString())

    def read(value: JsValue) = value match {
      case JsString(x) => new URI(x)
      case x => deserializationError("Expected URI as JsString, but got " + x)
    }
  }
}


/**
 * Flattens the JSON representation of a case class that contains a single `value`
 * element from:
 *
 * {"value": "..."}
 *
 * to `"..."`
 */
case class SingleValueCaseClassFormat[T <: {def value : V}, V](construct: V => T)(implicit delegate: JsonFormat[V]) extends RootJsonFormat[T] {

  import scala.language.reflectiveCalls
  override def write(obj: T) = delegate.write(obj.value)

  override def read(json: JsValue) = construct(delegate.read(json))
}


// Marshaller for innocent case classes that don't have any parameters
// assumes that the case classes behave like singletons
// https://github.com/spray/spray-json/issues/41
case class NoParamCaseClassFormat[T](instance: T) extends RootJsonFormat[T] {

  override def write(obj: T) = JsString(instance.getClass.getSimpleName)

  override def read(json: JsValue) = json match {
    case JsString(x) =>
      if(x != instance.getClass.getSimpleName)
        deserializationError("Expected %s, but got %s" format (instance.getClass.getSimpleName, x))
      instance
    case x => deserializationError("Expected JsString, but got " + x)
  }
}

// nulls are used in some Mongo Queries, so don't forget to import this
trait NullMarshalling {
  implicit protected val NullFormat = new RootJsonFormat[Null] {
    def write(obj: Null) = JsNull
    def read(json: JsValue) = null
  }
}