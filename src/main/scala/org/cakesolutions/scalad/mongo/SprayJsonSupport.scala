package org.cakesolutions.scalad.mongo

import spray.json._
import com.mongodb._
import org.bson.types._
import java.util.{Date, UUID}
import java.text.{ParseException, SimpleDateFormat}
import java.net.URI
import com.typesafe.config.ConfigFactory
import scala.Some
import util.JSON
import java.math.BigInteger


/** Convenience that allows a collection to be setup as using Spray JSON marshalling */
trait IndexedCollectionSprayJson[T] extends SprayJsonSerialisation[T] with IndexedCollectionProvider[T]


/** Mixin to get an implicit SprayJsonSerialisation in scope. */
trait SprayJsonSerializers {
  implicit def sprayJsonSerializer[T: JsonFormat]: MongoSerializer[T] = new SprayJsonSerialisation[T]
}

/** Mixin that gives an implicit conversion from String to DBObject. */
trait MongoQueries {
  import scala.language.implicitConversions
  protected implicit def stringToMongo(query: String) = JSON.parse(query).asInstanceOf[DBObject]
}
/** Importable that gives an implicit conversion from String to DBObject. */
object ImplicitMongoQueries extends MongoQueries {
  import scala.language.implicitConversions
  implicit def StringToMongo(query: String) = stringToMongo(query)
}

/** Uses `spray-json` to serialise/deserialise database objects
  * directly from `JsObject` -> `DBObject`.
  *
  * 1. `UUID` is treated as a special case and stored appropriately.
  * 2. MongoDB does not have support for arbitrary precision numbers.
  */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  import SprayJsonConvertors._

  override def serialize(entity: T): Object = {
    js2db(formatter.write(entity))
  }

  override def deserialize(found: Object): T = {
    formatter.read(obj2js(found))
  }

  def formatter = implicitly[JsonFormat[T]]
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
    case x => deserializationError("Expected JsObject, got %s" format(x))
  }

}

trait UuidMarshalling {

  implicit object UuidJsonFormat extends BsonMarshalling[UUID] with UuidChecker {

    override val key = "$uuid"
    override def writeString(obj: UUID) = obj.toString
    override def readString(value: String) = parseUuidString(value) match {
      case None => deserializationError("Expected UUID format, got %s" format(value))
      case Some(uuid) => uuid
    }
  }

}

trait DateMarshalling {

  implicit object DateJsonFormat extends BsonMarshalling[Date] with IsoDateChecker {

    override val key = "$date"
    override def writeString(obj: Date) = dateToIsoString(obj)
    override def readString(value: String) = parseIsoDateString(value) match {
      case None => deserializationError("Expected ISO Date format, got %s" format(value))
      case Some(date) => date
    }
  }
}

/** [[scala.math.BigDecimal]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
case class StringBigDecimal(value: BigDecimal)

object StringBigDecimal {
  def apply(value: String) = new StringBigDecimal(BigDecimal(value))
}

/** [[scala.math.BigInt]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
case class StringBigInt(value: BigInt)

object StringBigInt {
  def apply(value: String) = new StringBigInt(BigInt(value))
}

/** Alternative to [[spray.json.BasicFormats]] `JsNumber` marshalling. */
trait BigNumberMarshalling {

  implicit object StringBigDecimalJsonFormat extends BsonMarshalling[StringBigDecimal] {

    override val key = "StringBigDecimal"
    override def writeString(obj: StringBigDecimal) = obj.value.toString()
    override def readString(value: String) = try StringBigDecimal(value)
      catch {
        case e: NumberFormatException =>
          deserializationError("Expected StringBigDecimal format, got %s" format(value))
      }
  }

  implicit object StringBigIntJsonFormat extends BsonMarshalling[StringBigInt] {

    override val key = "StringBigInt"
    override def writeString(obj: StringBigInt) = obj.value.toString()
    override def readString(value: String) = try StringBigInt(value)
    catch {
      case e: NumberFormatException =>
        deserializationError("Expected StringBigInt format, got %s" format(value))
    }
  }

}


object SprayJsonConvertors extends SprayJsonConvertors

class SprayJsonConvertors extends UuidChecker with J2SELogging
  with UuidMarshalling with DateMarshalling {

  def js2db(jsValue: JsValue): Object = {
    import scala.collection.convert.WrapAsJava._

    jsValue match {
      case JsString(s) => s
      case JsNumber(n) =>
        // MongoDB doesn't support arbitrary precision numbers
        if (n.isValidLong)
          new java.lang.Long(n.toLong)
        else {
          // https://issues.scala-lang.org/browse/SI-6699
          val d = n.toDouble
          if (n != BigDecimal(d))
            log.config("Lost precision from " + n + " to " + d)
          new java.lang.Double(d)
        }
      case JsNull => null
      case JsBoolean(b) => Boolean.box(b)
      case a: JsArray =>
        val list = new BasicDBList()
        list.addAll(a.elements.map(f => js2db(f)))
        list
      case o: JsObject =>
        val fields = o.fields
        if (fields.contains("$date")) o.convertTo[Date]
        else if (fields.contains("$uuid")) o.convertTo[UUID]
        else new BasicDBObject(fields.map(f => (f._1, js2db(f._2))).toMap)
    }
  }

  def obj2js(obj: Object): JsValue = {
    import scala.language.postfixOps
    import scala.collection.convert.WrapAsScala._

    obj match {
      case a: BasicDBList => JsArray(a.toList.map {
        f => obj2js(f)
      })
      case dbObj: BasicDBObject =>
        val javaMap = dbObj.toMap.asInstanceOf[java.util.Map[String, Object]]
        JsObject(javaMap.map {
          f => (f._1, obj2js(f._2))
        } toMap)
      case objId: ObjectId => JsString(objId.toString)
      case s: java.lang.String => JsString(s)
      case b: java.lang.Boolean => JsBoolean(b)
      case i: java.lang.Integer => JsNumber(i)
      case l: java.lang.Long => JsNumber(l)
      case d: java.lang.Double => JsNumber(d)
      case date: java.util.Date => date.toJson
      case uuid: java.util.UUID => uuid.toJson
      case null => JsNull
      case unsupported =>
        throw new UnsupportedOperationException("Deserializing " + unsupported.getClass + ": " + unsupported)
    }
  }
}

trait UuidChecker {
  def parseUuidString(token: String): Option[UUID] = {
    if (token.length != 36) None
    else try Some(UUID.fromString(token))
    catch {
      case p: IllegalArgumentException => return None
    }
  }
}

trait IsoDateChecker {
  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)

  def parseIsoDateString(date: String): Option[Date] =
    if (date.length != 28) None
    else try Some(localIsoDateFormatter.get().parse(date))
    catch {
      case p: ParseException =>
        println(date + " " + p)
        None
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
 * {{{
 * {"value": "..."}
 * }}}
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

