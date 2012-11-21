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

//Bogus domain-like entities
case class Person(name: String, surname: String)
case class Address(road: String, number: Int)

case class Student(id: Int,
                   name: String,
                   parents: List[Person],
                   address: Address)

class SprayJsonSupportTest extends Specification with DefaultJsonProtocol with MongoCrudTestAccess with UuidMarshalling {

  implicit val JsObjectEntityFormatter = jsonFormat1(JsValueEntity)
  implicit val DoubleEntityFormatter = jsonFormat1(DoubleEntity)
  implicit val PersonFormatter = jsonFormat2(Person)
  implicit val AddressFormatter = jsonFormat2(Address)
  implicit val StudentFormatter = jsonFormat4(Student)
  implicit val StudentSerialiser = new SprayJsonSerialisation[Student]

  implicit val StudentCollectionProvider = new IndexedCollectionProvider[Student] {
    override def getCollection = db.getCollection("student")
    override def uniqueFields = "{'id': 1}" :: Nil
  }

  def mustSerialize[T: JsonFormat](entity: T, expected: DBObject) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.serialize(entity) must beEqualTo(expected)
  }

  /* Use this if you want to make sure you get back the same entity
   * from the entire serialization/deserialization process.
   */
  def mustSerializeAndDeserialize[T: JsonFormat](entity: T) {
      val serializer = new SprayJsonSerialisation[T]
      serializer.deserialize(serializer.serialize(entity)) must beEqualTo(entity)
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

    "be able to ONLY serialize a nested Map" in {
      //Caveat: If you want to test in isolation only the serializer,
      //you can't simply create a new BasicDBObject out of the Scala
      //nested Map. You need to embed the nested Map into a BasicDBObject as well!
      val nested = Map("b" -> "c")
      val original = Map("a" -> nested)
      val expected = new BasicDBObject()
      expected.put("a", new BasicDBObject(nested))
      mustSerialize(original, expected)
    }

    "be able to serialize/deserialize a nested Map" in {
      val original = Map("a" -> Map("b" -> Map("c" -> "!")))
      mustSerializeAndDeserialize(original)
    }

    "be able to serialize a Person" in {
      val original = Person("John", "Doe")
      val expected = new BasicDBObject(Map("name" -> "John",
                                           "surname" -> "Doe"))
      mustSerialize(original, expected)
    }

    "be able to deserialize a Person" in {
      mustSerializeAndDeserialize(Person("John", "Doe"))
    }

    "be able to serialize/deserialize a Student" in {
      val original = Student(101287
                             ,"Alfredo"
                             ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
                             ,Address("Foo Rd.", 91)
                             )
      mustSerializeAndDeserialize(original)
    }
  }

  "SprayJsonSerialisation" should {
    sequential

    val crud = new MongoCrud
    val jsonQuery = "{'id': 87}"
    val original = Student(87
                           ,"Alfredo"
                           ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
                           ,Address("Foo Rd.", 91)
                           )
    val update = Student(87
                         ,"Di Napoli"
                         ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
                         ,Address("Foo Rd.", 91)
                         )

    "ensure a student is correctly persisted" in {
      crud.create(original).get must beEqualTo(original)
    }
  }
}
