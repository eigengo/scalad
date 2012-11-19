package org.cakesolutions.scalad.mongo

import spray.json.{JsValue, JsObject, JsonParser, JsonFormat}
import spray.json.{JsArray, JsBoolean, JsString, JsNull, JsNumber}
import com.mongodb.{util, BasicDBObject, DBObject}

import scala.annotation.tailrec

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


/**
 * Uses `spray-json` to serialise/deserialise database objects
 * directly from `JsObject` -> `DBObject`.
 *
 * Note: `UUID` objects are persisted as `String`s not BSON.
 */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  override def serialize(entity: T): DBObject = {
    
    import scala.collection.JavaConversions.mapAsJavaMap
    val formatter = implicitly[JsonFormat[T]]
    val jsObject = formatter.write(entity).asJsObject
    new BasicDBObject(expandJsObject(jsObject))
  }

  implicit def expandJsObject(o: JsObject): Map[String, Object] = {
    val partials = o.fields.toList.map { x => jsValue2Object(x._1, x._2) }
    partials.reduce { _ ++ _ }
  }

  def jsValue2Object(key: String, js: JsValue): Map[String, Object] = js match {
    case JsString(s) => Map(key -> s)
    case JsNumber(n) => Map(key -> n)
    case JsNull => Map(key -> None)
    case JsBoolean(b) => Map(key -> Boolean.box(b))
    case a@JsArray(_) => Map(key -> a.elements.map(jsValue2Object(key,_)).flatten.map { _._2 })
    case o@JsObject(_) => o
  }


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
