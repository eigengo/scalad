package org.cakesolutions.scalad.mongo

import com.mongodb._
import org.specs2._
import org.specs2.mutable.Specification
import spray.json._
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

case class IntEntity(value: Int)
case class StringEntity(value: String)
case class BooleanEntity(value: Boolean)
case class NullEntity(value: Option[String])
case class JsValueEntity(value: JsValue)

class SprayJsonSupportTest extends Specification with DefaultJsonProtocol {

  implicit val StringEntityFormatter = jsonFormat1(StringEntity)
  implicit val IntEntityFormatter = jsonFormat1(IntEntity)
  implicit val BooleanEntityFormatter = jsonFormat1(BooleanEntity)
  implicit val NullEntityFormatter = jsonFormat1(NullEntity)
  implicit val JsObjectEntityFormatter = jsonFormat1(JsValueEntity)


  def mustSerialize[T: JsonFormat](entity: T, expected: DBObject) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.serialize(entity) must beEqualTo(expected)
  }

  def mustDeserialize[T: JsonFormat](entity: T) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.deserialize(serializer.serialize(entity)) must beEqualTo(entity)
   }

  "Spray-Json-base serializer" should {

    "be able to serialize an Int" in {
      mustSerialize(IntEntity(20), new BasicDBObject(Map("value" -> 20)))
    }

    "be able to deserialize an Int" in {
      mustDeserialize(IntEntity(20))
    }

    "be able to serialize a Boolean" in {
      mustSerialize(BooleanEntity(true), new BasicDBObject(Map("value" -> true)))
    }

    "be able to deserialize an Boolean" in {
      mustDeserialize(BooleanEntity(false))
    }

    "be able to serialize a String" in {
      mustSerialize(StringEntity("hello"), new BasicDBObject(Map("value" -> "hello")))
    }

    "be able to deserialize a String" in {
      mustDeserialize(StringEntity("original"))
    }

  }
}
