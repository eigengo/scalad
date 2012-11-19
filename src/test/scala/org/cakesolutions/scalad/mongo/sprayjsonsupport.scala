package org.cakesolutions.scalad.mongo

import com.mongodb._
import org.specs2._
import org.specs2.mutable.Specification
import spray.json._
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

case class SerializableIntEntity(id: Long, word: Int)

class SprayJsonSupportTest extends Specification with DefaultJsonProtocol {

  implicit val LongEntityFormatter = jsonFormat2(LongEntity)
  implicit val GenericEntityFormatter = jsonFormat2(SerializableIntEntity)

  "Spray-Json-base serializer" should {

    "be able to serialize an Int" in {
      val serializer = new SprayJsonSerialisation[SerializableIntEntity]
      val e1 = SerializableIntEntity(10, 20)
      val expected = new BasicDBObject(Map("id" -> 10, "word" -> 20))
      serializer.serialize(e1) must beEqualTo(expected)
    }

    "be able to serialize a LongEntity" in {
      val serializer = new SprayJsonSerialisation[LongEntity]
      val e1 = LongEntity(23, "hello")
      val expected = new BasicDBObject(Map("id" -> 23, "word" -> "hello"))
      serializer.serialize(e1) must beEqualTo(expected)
    }

    "be able to deserialize a LongEntity" in {
      val serializer = new SprayJsonSerialisation[LongEntity]
      val e1 = LongEntity(99, "original")
      val expected = LongEntity(99, "original")
      serializer.deserialize(serializer.serialize(e1)) must beEqualTo(expected)
    }
  }
}
