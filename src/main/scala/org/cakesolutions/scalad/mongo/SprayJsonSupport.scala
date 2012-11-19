package org.cakesolutions.scalad.mongo

import spray.json.{JsValue, JsObject, JsonParser, JsonFormat}
import spray.json.{JsArray, JsBoolean, JsString, JsNull, JsNumber}
import com.mongodb._

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
    import scala.collection.JavaConversions.mapAsJavaMap
    import scala.collection.JavaConversions.seqAsJavaList

    jsValue match {
      case JsString(s) => s // do some magic with mixins for special forms (UUID, Date, etc)
      case JsNumber(n) => n
      case JsNull => None
      case JsBoolean(b) => Boolean.box(b)
      case a: JsArray => {
        val list = new BasicDBList()
        list.addAll(a.elements.map(f => js2db(f)))
        list
      }
      case o: JsObject =>  new BasicDBObject(o.fields.map(f => (f._1, js2db(f._2))).toMap)
    }
  }

  implicit val SprayJsonToDBObject = (jsValue: JsValue) => js2db(jsValue).asInstanceOf[DBObject]

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

  def formatter = implicitly[JsonFormat[T]]

  override def deserialize(found: DBObject): T = {

    val jsObject = JsObject(found.toMap().asInstanceOf[Map[String, Object]])
    val formatter = implicitly[JsonFormat[T]]
    formatter.read(jsObject)
  }

  implicit def objMap2JsValueMap(found: Map[String, Object]): Map[String, JsValue] = {
    var res: Map[String, JsValue] = Map()
    for((k, o) <- found) {
      o match {
        case s: java.lang.String => res = res ++ Map(k -> JsString(s))
        case b: java.lang.Boolean => res = res ++ Map(k -> JsBoolean(b))
        case i: Integer => res = res ++ Map(k -> JsNumber(i))
        case l: java.lang.Long => res ++ Map(k -> JsNumber(l))
        case d: java.lang.Double => res = res ++ Map(k -> JsNumber(d))
        case bi: BigInt => res = res ++ Map(k -> JsNumber(bi))
      }
    }

    res
  }
}
