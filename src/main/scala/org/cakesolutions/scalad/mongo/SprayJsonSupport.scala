package org.cakesolutions.scalad.mongo

import spray.json.{JsValue, JsObject, JsonParser, JsonFormat}
import spray.json.{JsArray, JsBoolean, JsString, JsNull, JsNumber}
import com.mongodb._


trait UuidChecker {
  // http://en.wikipedia.org/wiki/Universally_unique_identifier
  val uuidRegex = """^\p{XDigit}{8}(-\p{XDigit}{4}){3}-\p{XDigit}{12}$""".r

  def isValidUuid(token: String) = {
    token.length == 36 && uuidRegex.findPrefixOf(token).isDefined
  }
}

/**
 * Uses `spray-json` to serialise/deserialise database objects via
 * an intermediary `String` stage to force a JSON representation of
 * the objects.
 *
 * Note: `UUID` objects are persisted as `String`s not BSON.
 */
class SprayJsonStringSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  override def serialize(entity: T): DBObject = {
    val formatter = implicitly[JsonFormat[T]]
    val json = formatter.write(entity).compactPrint
    util.JSON.parse(json).asInstanceOf[DBObject] // see JSON.parse and cry – this works for well formed JSON
  }

  override def deserialize(found: DBObject): T = {
    val json = util.JSON.serialize(found)
    val parsed = JsonParser.apply(json)
    val formatter = implicitly[JsonFormat[T]]
    formatter.read(parsed)
  }
}

/**
 * Mix this in to automatically get an implicit SprayJsonStringSerialisation in your scope
 */
trait SprayJsonStringSerializers {

  /**
   * Construct MongoSerializer for A, if there is an instance of JsonWriter for A
   */
  implicit def sprayJsonStringSerializer[T: JsonFormat]: MongoSerializer[T] = new SprayJsonStringSerialisation[T]
}


object SprayJsonImplicits {

  def js2db(jsValue: JsValue): Object = {
    import scala.collection.convert.WrapAsJava._
    import scala.collection.convert.WrapAsScala._

    jsValue match {
      case JsString(s) => s // do some magic with mixins for special forms (UUID, Date, etc)
      case JsNumber(n) => n.bigDecimal
      case JsNull => null
      case JsBoolean(b) => Boolean.box(b)
      case a: JsArray => {
        val list = new BasicDBList()
        list.addAll(a.elements.map(f => js2db(f)))
        list
      }
      case o: JsObject =>  new BasicDBObject(o.fields.map(f => (f._1, js2db(f._2))).toMap)
    }
  }

  def obj2js(obj: Object) : JsValue = {
    import scala.collection.convert.WrapAsJava._
    import scala.collection.convert.WrapAsScala._
    import scala.language.postfixOps

    obj match {
      case a: BasicDBList => {
        val content: Array[JsValue] = a.toArray.map { f => obj2js(f) } 
        JsArray(content: _*)
      }

      case dbObj: BasicDBObject => {
        val javaMap = dbObj.toMap().asInstanceOf[java.util.Map[String, Object]]
        JsObject(javaMap.map {f => (f._1, obj2js(f._2))} toMap)
      }

      case s: java.lang.String => JsString(s)
      case uuid: java.util.UUID => JsString(uuid.toString())
      case b: java.lang.Boolean => JsBoolean(b)
      case i: java.lang.Integer => JsNumber(i)
      case l: java.lang.Long => JsNumber(l)
      case bi: java.math.BigInteger => JsNumber(bi)
      case bd: java.math.BigDecimal => JsNumber(bd)
      case null => JsNull
      case otherwise => {
        val errMsg = "[%s]: No known deserialization for %s.".format(otherwise.getClass, otherwise)
        throw new UnsupportedOperationException(errMsg)
      }
    }
  }

  implicit val SprayJsonToDBObject = (jsValue: JsValue) => js2db(jsValue).asInstanceOf[DBObject]

  implicit val ObjectToSprayJson = (obj: DBObject) => obj2js(obj)

  implicit val SprayStringToDBObject = (json: String) => js2db(JsonParser.apply(json))
}

/**
 * Uses `spray-json` to serialise/deserialise database objects
 * directly from `JsObject` -> `DBObject`.
 *
 * Note: `UUID` objects are persisted as `String`s not BSON.
 */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  import SprayJsonImplicits._

  override def serialize(entity: T): DBObject = {
    formatter.write(entity)
  }

  override def deserialize(found: DBObject): T = {
    formatter.read(found)
  }

  def formatter = implicitly[JsonFormat[T]]
}
