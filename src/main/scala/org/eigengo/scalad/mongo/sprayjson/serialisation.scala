package org.eigengo.scalad.mongo.sprayjson

import spray.json._
import scala._
import com.mongodb.{BasicDBObject, BasicDBList}
import java.util.{UUID, Date}
import org.bson.types.ObjectId
import org.eigengo.scalad.mongo.{UuidChecker, MongoSerialiser}
import akka.contrib.jul.JavaLogging

/** Uses `spray-json` to serialise/deserialise database objects
  * directly from `JsObject` -> `DBObject`.
  *
  * 1. `UUID` and `Date` are treated as special cases and stored using native types.
  * 2. MongoDB does not have support for arbitrary precision numbers, see
  *    [[org.eigengo.scalad.mongo.sprayjson.BigNumberMarshalling]].
  */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerialiser[T] with SprayJsonConvertors with JavaLogging {

  override def serialise(entity: T): Object = js2db(implicitly[JsonFormat[T]].write(entity))

  override def deserialise(found: Object): T = implicitly[JsonFormat[T]].read(obj2js(found))
}

trait SprayJsonConvertors extends UuidChecker with UuidMarshalling with DateMarshalling {
  this: JavaLogging =>

  protected def js2db(jsValue: JsValue): Object = {
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
            log.info("Lost precision from " + n + " to " + d)
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

  protected def obj2js(obj: Object): JsValue = {
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
        throw new UnsupportedOperationException("Deserialising " + unsupported.getClass + ": " + unsupported)
    }
  }
}

