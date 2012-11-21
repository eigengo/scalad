package org.cakesolutions.scalad.mongo

import com.mongodb._
import java.util.UUID
import org.specs2._
import org.specs2.mutable.Specification
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import spray.json._

case class JsValueEntity(value: JsValue)
case class DoubleEntity(value: Double)

class SprayJsonSupportTest extends Specification with DefaultJsonProtocol with UuidMarshalling {

  implicit val JsObjectEntityFormatter = jsonFormat1(JsValueEntity)
  implicit val DoubleEntityFormatter = jsonFormat1(DoubleEntity)

  def mustSerialize[T: JsonFormat](entity: T, expected: DBObject) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.serialize(entity) must beEqualTo(expected)
  }

  def mustDeserialize[T: JsonFormat](entity: DBObject, expected: T) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.deserialize(entity) must beEqualTo(expected)
   }

  def mustFailToDeserializeWith[T: JsonFormat, E <: Throwable: Manifest](entity: DBObject) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.deserialize(entity) must throwA[E]
   }

  "Spray-Json-base serializer" should {

    "be able to serialize an Int" in {
      val original = Map("value" -> 20)
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize an Int" in {
      val original = Map("v" -> 20)
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a Double" in {
      val original = Map("v" -> 1.23)
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to fail if asked to deserialize a raw Double" in {
      val obj = new BasicDBObject(Map("value" -> 10.1))
      mustFailToDeserializeWith[Double, UnsupportedOperationException](obj)
    }

    "be able to serialize a Long" in {
      val original = Map("value" -> 200.toLong)
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize a Long" in {
      val original = Map("v" -> 200.toLong)
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a Boolean" in {
      val original = Map("v" -> true)
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize an Boolean" in {
      val original = Map("v" -> true) 
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a String" in {
      val original = Map("value" -> "hello")
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize a String" in {
      val original = Map("v" -> "hello")
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a UUID" in {
      val original = Map("v" -> UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize a UUID" in {
      val original = Map("v" -> UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a JsNull" in {
      mustSerialize(JsValueEntity(JsNull), new BasicDBObject("value", null))
    }

    "be able to deserialize a JsNull" in {
      val original = JsValueEntity(JsNull)
      mustDeserialize(new BasicDBObject("value", null), original)
    }

    "be able to serialize an homogeneous List" in {
      val a1 = List("a", "b", "c")
      val dbList = new BasicDBList()
      dbList.addAll(a1)
      val expected = new BasicDBObject(Map("value" -> dbList))
      mustSerialize(Map("value" -> a1), expected)
    }

    "be able to deserialize an homogeneous List" in {
      val a1 = List("a", "b", "c")
      val dbList = new BasicDBList()
      dbList.addAll(a1)
      val original = dbList
      mustDeserialize(original, a1)
    }

    "be able to serialize an heterogeneous List" in {
      //Should the API be able to serialize heterogeneous List, like
      //List(null, 10.12, "hello")?
      todo
    }

    "be able to serialize a Map" in {
      val original = Map("key" -> "value")
      mustSerialize(original, new BasicDBObject(original))
    }

    "be able to deserialize a Map" in {
      val original = Map("key" -> "value")
      mustDeserialize(new BasicDBObject(original), original)
    }

    "be able to serialize a nested Map" in {
      //Should the API be able to serialize/deserialize nested Maps,
      //for example Map("a" -> Map("b" -> Map("c" -> "!")))
      todo
    }
  }
}
