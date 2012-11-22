package org.cakesolutions.scalad.mongo

import spray.json._
import com.mongodb._
import org.bson.types._
import java.util.UUID


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


object SprayJsonImplicits extends UuidChecker{

  def js2db(jsValue: JsValue): Object = {
    import scala.collection.convert.WrapAsJava._
    import scala.collection.convert.WrapAsScala._

    jsValue match {
      case JsString(s) => {
        if (isValidUuid(s))
          UUID.fromString(s)
        else s // do some magic with mixins for special forms (Date, etc)
      }
      case JsNumber(n) => {
        //We need to do such a checks because Mongo doesn't support BigDecimals
        if (n.isValidLong)
          new java.lang.Long(n.toLong)

        //See here why we used this condition:
        //https://issues.scala-lang.org/browse/SI-6699
        else if (n == BigDecimal(n.toDouble))
          new java.lang.Double(n.toDouble)
        else throw new UnsupportedOperationException("Serializing "+ n.getClass + ": " + n) 
      }
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

    obj match {
      case a: BasicDBList => JsArray(a.toList.map { f => obj2js(f) })
      case dbObj: BasicDBObject => {
        val javaMap = dbObj.toMap.asInstanceOf[java.util.Map[String, Object]]
        JsObject(javaMap.map {f => (f._1, obj2js(f._2))} toMap)
      }
      case objId: ObjectId => JsString(objId.toString)
      case s: java.lang.String => JsString(s)
      case uuid: java.util.UUID => JsString(uuid.toString)
      case b: java.lang.Boolean => JsBoolean(b)
      case i: java.lang.Integer => JsNumber(i)
      case l: java.lang.Long => JsNumber(l)
      case bi: java.math.BigInteger => JsNumber(bi)
      case bd: java.math.BigDecimal => JsNumber(bd)
      case null => JsNull
      case unsupported => throw new UnsupportedOperationException("Deserializing "+ unsupported.getClass + ": " + unsupported)
    }
  }

  implicit val SprayJsonToDBObject = (jsValue: JsValue) => js2db(jsValue).asInstanceOf[DBObject]
  implicit val ObjectToSprayJson = (obj: DBObject) => obj2js(obj)
  implicit val SprayStringToDBObject = (json: String) => SprayJsonToDBObject.apply(JsonParser.apply(json))
}

/**
 * Mix this in to automatically get an implicit SprayJsonSerialisation in your scope
 */
trait SprayJsonSerializers {
  implicit def sprayJsonSerializer[T: JsonFormat]: MongoSerializer[T] = new SprayJsonSerialisation[T]
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
