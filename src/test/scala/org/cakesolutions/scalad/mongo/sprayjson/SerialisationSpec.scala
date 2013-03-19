package org.cakesolutions.scalad.mongo.sprayjson

import org.specs2.mutable.Specification
import com.mongodb.{DBObject, BasicDBList, BasicDBObject}
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import java.util.{Date, UUID}
import spray.json.{JsonFormat, JsNull}

class SerialisationSpec extends Specification with SprayJsonTestSupport {

  def mustSerialize[T: JsonFormat](entity: T, expected: Object) {
    val serializer = new SprayJsonSerialisation[T]
    serializer.serialise(entity) must beEqualTo(expected)
  }

  /* Use this if you want to make sure you get back the same entity
   * from the entire serialization/deserialization process.
   */
  def mustSerializeAndDeserialize[T: JsonFormat](entity: T) {
    val serializer = new SprayJsonSerialisation[T]
    serializer.deserialise(serializer.serialise(entity)) must beEqualTo(entity)
  }

  def mustDeserialize[T: JsonFormat](entity: Object, expected: T) {
    val serializer = new SprayJsonSerialisation[T]
    serializer.deserialise(entity) must beEqualTo(expected)
  }

  def mustFailToDeserializeWith[T: JsonFormat, E <: Throwable : Manifest](entity: DBObject) {
    val serializer = new SprayJsonSerialisation[T]
    serializer.deserialise(entity) must throwA[E]
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

    "be able to serialize a complex Double" in {
      val original = Map("v" -> 3.141592653589793238462643383279502884197169399)
      mustSerialize(original, new BasicDBObject(original))
    }


    "be able to deserialize a raw Double" in {
      val original = Map("value" -> 10.1)
      mustDeserialize(new BasicDBObject(original), original)
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
      val string = "550e8400-e29b-41d4-a716-446655440000"
      val json = Map("$uuid" -> string)
      mustSerialize(json, UUID.fromString(string))
    }

    "be able to deserialize a UUID" in {
      val string = "550e8400-e29b-41d4-a716-446655440000"
      val json = Map("$uuid" -> string)
      mustDeserialize(UUID.fromString(string), json)
    }

    "be able to serialise a Date" in {
      val json = Map("$date" -> "2013-02-04T17:51:35.479+0000")
      mustSerialize(json, new Date(1360000295479L))
    }

    "be able to deserialise a Date" in {
      val json = Map("$date" -> "2013-02-04T17:51:35.479+0000")
      mustDeserialize(new Date(1360000295479L), json)
    }

    //    "be able to serialise a StringBigDecimal" in {
    //      val string = "100000000000000.00000000000001"
    //      val original = StringBigDecimal(string)
    //      val expected:DBObject = new BasicDBObject("StringBigDecimal", string)
    //      mustSerialize(original, expected)
    //    }
    //
    //    "be able to deserialise a StringBigDecimal" in {
    //      val string = "100000000000000.00000000000001"
    //      val original = StringBigDecimal(string)
    //      val expected = new BasicDBObject("StringBigDecimal", string)
    //      mustDeserialize(expected, original)
    //    }
    //
    //    "be able to serialise a StringBigInt" in {
    //      val string = "10000000000000000000000000001"
    //      val original = StringBigInt(string)
    //      val expected:DBObject = new BasicDBObject("StringBigInt", string)
    //      mustSerialize(original, expected)
    //    }
    //
    //    "be able to deserialise a StringBigInt" in {
    //      val string = "10000000000000000000000000001"
    //      val original = StringBigInt(string)
    //      val expected = new BasicDBObject("StringBigInt", string)
    //      mustDeserialize(expected, original)
    //    }

    "be able to serialise a StringBigDecimal" in {
      val string = "100000000000000.00000000000001"
      val original = StringBigDecimal(string)
      mustSerialize(original, string)
    }

    "be able to serialise a StringBigInt" in {
      val string = "10000000000000000000000000001"
      val original = StringBigInt(string)
      mustSerialize(original, string)
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
      val uuid = UUID.randomUUID()
      val original = Student(101287
        ,uuid
        ,"Alfredo"
        ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
        ,Address("Foo Rd.", 91)
        ,graduated = false
      )
      mustSerializeAndDeserialize(original)
    }
  }

}
