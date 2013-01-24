package org.cakesolutions.scalad.mongo

import spray.json._
import com.mongodb._
import org.bson.types._
import java.util.{Date, UUID}
import java.text.{ParseException, SimpleDateFormat}
import java.net.URI


/** Convenience that allows a collection to be setup as using Spray JSON marshalling */
trait IndexedCollectionSprayJson[T] extends SprayJsonSerialisation[T] with IndexedCollectionProvider[T]


/** Mixin to get an implicit SprayJsonSerialisation in scope. */
trait SprayJsonSerializers {
  implicit def sprayJsonSerializer[T: JsonFormat]: MongoSerializer[T] = new SprayJsonSerialisation[T]
}

/** Uses `spray-json` to serialise/deserialise database objects
  * directly from `JsObject` -> `DBObject`.
  *
  * 1. `UUID` is treated as a special case and stored appropriately.
  * 2. MongoDB does not have support for arbitrary precision numbers.
  */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  import SprayJsonImplicits.{js2db, obj2js}

  override def serialize(entity: T): Object = {
    js2db(formatter.write(entity))
  }

  override def deserialize(found: Object): T = {
    formatter.read(obj2js(found))
  }

  def formatter = implicitly[JsonFormat[T]]
}

object SprayJsonImplicits extends SprayJsonImplicits {

  import scala.language.implicitConversions

  // SprayJsonToDBObject will fail for trivial serialisations (e.g. a single `JsString`)
  implicit val SprayJsonToDBObject = (jsValue: JsValue) => js2db(jsValue).asInstanceOf[DBObject]
  implicit val DBObjectToSprayJson = (obj: Object) => obj2js(obj)
  implicit val SprayStringToDBObject = (json: String) => SprayJsonToDBObject(JsonParser(json))
}


class SprayJsonImplicits extends UuidChecker with J2SELogging {
  def js2db(jsValue: JsValue): Object = {
    import scala.collection.convert.WrapAsJava._

    jsValue match {
      case JsString(s) =>
        if (isValidUuid(s))
          UUID.fromString(s)
        else s
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
      case o: JsObject => new BasicDBObject(o.fields.map(f => (f._1, js2db(f._2))).toMap)
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
      case uuid: java.util.UUID => JsString(uuid.toString)
      case b: java.lang.Boolean => JsBoolean(b)
      case i: java.lang.Integer => JsNumber(i)
      case l: java.lang.Long => JsNumber(l)
      case d: java.lang.Double => JsNumber(d)
      case null => JsNull
      case unsupported => throw new UnsupportedOperationException("Deserializing " + unsupported.getClass + ": " + unsupported)
    }
  }
}

trait UuidChecker {
  // http://en.wikipedia.org/wiki/Universally_unique_identifier
  val uuidRegex = """^\p{XDigit}{8}(-\p{XDigit}{4}){3}-\p{XDigit}{12}$""".r

  def isValidUuid(token: String) = {
    token.length == 36 && uuidRegex.findPrefixOf(token).isDefined
  }
}

// might move upstream: https://github.com/spray/spray-json/issues/25
trait UuidMarshalling {

  implicit object UuidJsonFormat extends JsonFormat[UUID] {
    def write(x: UUID) = JsString(x toString())

    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
}

trait JavaDateStringMarshalling {

  implicit object JavaDateStringJsonFormat extends JsonFormat[Date] {

    private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    def write(obj: Date) = formatter.synchronized(JsString(formatter.format(obj)))

    def read(json: JsValue) = json match {
      case JsString(text) =>
        try formatter.synchronized(formatter.parse(text))
        catch {
          case ParseException => deserializationError("Unexpected DateFormat: " + text)
        }
      case x => deserializationError("Expected Date as JsNumber, but got " + x)
    }
  }
}


trait JavaDateLongMarshalling {

  implicit protected object DateJsonFormat extends JsonFormat[Date] {
    def write(x: Date) = JsNumber(x.getTime)

    def read(value: JsValue) = value match {
      case JsNumber(x) => new Date(x.toLong)
      case x => deserializationError("Expected Date as JsNumber, but got " + x)
    }
  }

}


trait UriMarshalling {

  implicit protected object UriJsonFormat extends JsonFormat[URI] {
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
case class SingleValueCaseClassFormat[T <: {def value : V}, V](construct: V => T)(implicit delegate: JsonFormat[V]) extends JsonFormat[T] {

  import scala.language.reflectiveCalls
  override def write(obj: T) = delegate.write(obj.value)

  override def read(json: JsValue) = construct(delegate.read(json))
}


// Marshaller for innocent case classes that don't have any parameters
// assumes that the case classes behave like singletons
// https://github.com/spray/spray-json/issues/41
case class NoParamCaseClassFormat[T](instance: T) extends JsonFormat[T] {

  override def write(obj: T) = JsString(instance.getClass.getSimpleName)

  override def read(json: JsValue) = json match {
    case JsString(x) =>
      if(x != instance.getClass.getSimpleName)
        deserializationError("Expected %s, but got %s" format (instance.getClass.getSimpleName, x))
      instance
    case x => deserializationError("Expected JsString, but got " + x)
  }
}
