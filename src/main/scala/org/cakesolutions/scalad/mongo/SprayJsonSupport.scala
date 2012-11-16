package org.cakesolutions.scalad.mongo

import spray.json.{JsValue, JsObject, JsonParser, JsonFormat}
import spray.json.{JsArray, JsString, JsNull, JsNumber, JsTrue, JsFalse}
import com.mongodb.{util, BasicDBObject, DBObject}

import scala.collection.JavaConversions.mapAsJavaMap
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
    val formatter = implicitly[JsonFormat[T]]
    val jsObject = formatter.write(entity).asJsObject

    //I don't like this chain of data structure transformation
    new BasicDBObject(jsObject2Map(jsObject, jsObject.fields.keys.toSeq, Map()))
  }

  //Not tailrecursive, for now.
  //This is currently broken, we need to find a way to keep the original
  //"root" object, but still recurring on the current JsValue.
  private def jsObject2Map(obj: JsObject, 
                           vals: Seq[String],
                           acc: Map[String, Object]): Map[String, Object] = vals match {
    case Seq() => acc
    case Seq(x, xs@_*) => obj.fields.get(x) match {
      case Some(JsArray(a))  => {
        val arrayFields = ???
        //convertJsVals(obj, xs, a.map { convertJsVals(_, arrayFields, acc) } fold {(m1: Map[String,Object], m2: Map[String, Object]) =>  m1 ++ m2 } ++ acc)
        ???
      }
      case Some(o@JsObject(_)) => {
        val objFields = o.fields.keys.toSeq
        jsObject2Map(obj, xs, jsObject2Map(o, objFields, acc))
      }
      case Some(JsString(s)) => jsObject2Map(obj, xs, Map(x -> s) ++ acc)
      case Some(JsNumber(n)) => jsObject2Map(obj, xs, Map(x -> n) ++ acc)
      case Some(JsNull)      => jsObject2Map(obj, xs, Map(x -> None) ++ acc)
      //Probably can be collapsed
      case Some(JsTrue)      => jsObject2Map(obj, xs, Map(x -> Boolean.box(true)) ++ acc)
      case Some(JsFalse)     => jsObject2Map(obj, xs, Map(x -> Boolean.box(false)) ++ acc)

      //should fail somehow
      case _ => ???
    }
  }

  override def deserialize(found: DBObject): T = {
    val jsObject = ???
    val formatter = implicitly[JsonFormat[T]]
    formatter.read(jsObject)
  }
}
