package org.eigengo.scalad.mongo.sprayjson

import org.specs2.mutable.Specification
import com.mongodb.{DBObject, BasicDBList, BasicDBObject}
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import java.util.{Date, UUID}
import spray.json.{JsonFormat, JsNull}
import org.joda.time.DateTime
import org.bson.types.ObjectId

class SerialisationSpec extends Specification with SprayJsonTestSupport {

  def mustSerialise[T: JsonFormat](entity: T, expected: Object) {
    val serialiser = new SprayJsonSerialisation[T]
    serialiser.serialise(entity) must beEqualTo(expected)
  }

  /* Use this if you want to make sure you get back the same entity
   * from the entire serialisation/deserialisation process.
   */
  def mustSerialiseAndDeserialise[T: JsonFormat](entity: T) {
    val serialiser = new SprayJsonSerialisation[T]
    serialiser.deserialise(serialiser.serialise(entity)) must beEqualTo(entity)
  }

  def mustDeserialise[T: JsonFormat](entity: Object, expected: T) {
    val serialiser = new SprayJsonSerialisation[T]
    serialiser.deserialise(entity) must beEqualTo(expected)
  }

  def mustFailToDeserialiseWith[T: JsonFormat, E <: Throwable : Manifest](entity: DBObject) {
    val serialiser = new SprayJsonSerialisation[T]
    serialiser.deserialise(entity) must throwA[E]
  }

  "Spray-Json-base serialiser" should {

    "be able to serialise an Int" in {
      val original = Map("value" -> 20)
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to deserialise an Int" in {
      val original = Map("v" -> 20)
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to serialise a Double" in {
      val original = Map("v" -> 1.23)
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to serialise a complex Double" in {
      val original = Map("v" -> 3.141592653589793238462643383279502884197169399)
      mustSerialise(original, new BasicDBObject(original))
    }


    "be able to deserialise a raw Double" in {
      val original = Map("value" -> 10.1)
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to serialise a Long" in {
      val original = Map("value" -> 200.toLong)
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to deserialise a Long" in {
      val original = Map("v" -> 200.toLong)
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to serialise a Boolean" in {
      val original = Map("v" -> true)
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to deserialise an Boolean" in {
      val original = Map("v" -> true)
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to serialise a String" in {
      val original = Map("value" -> "hello")
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to deserialise a String" in {
      val original = Map("v" -> "hello")
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to serialise a UUID" in {
      val string = "550e8400-e29b-41d4-a716-446655440000"
      val json = Map("$uuid" -> string)
      mustSerialise(json, UUID.fromString(string))
    }

    "be able to deserialise a UUID" in {
      val string = "550e8400-e29b-41d4-a716-446655440000"
      val json = Map("$uuid" -> string)
      mustDeserialise(UUID.fromString(string), json)
    }

    "be able to serialise an ObjectId" in {
      val oId = new ObjectId("53627de05ed0089ad3c1cdf9")
      val json = Map("$oid" -> oId.toString)
      mustSerialise(json, oId)
    }

    "be able to deserialise an ObjectId" in {
      val oId = new ObjectId("53627de05ed0089ad3c1cdf9")
      val json = Map("$oid" -> oId.toString)
      mustDeserialise(oId, json)
    }

    "be able to deserialise and serialize a DateTime" in {
      val d = new DateTime(1360000295479L)
      val serialiser = new SprayJsonSerialisation[DateTime]
      serialiser.serialise(serialiser.deserialise(d)) must beEqualTo(d)
    }

    //    "be able to serialise a StringBigDecimal" in {
    //      val string = "100000000000000.00000000000001"
    //      val original = StringBigDecimal(string)
    //      val expected:DBObject = new BasicDBObject("StringBigDecimal", string)
    //      mustSerialise(original, expected)
    //    }
    //
    //    "be able to deserialise a StringBigDecimal" in {
    //      val string = "100000000000000.00000000000001"
    //      val original = StringBigDecimal(string)
    //      val expected = new BasicDBObject("StringBigDecimal", string)
    //      mustDeserialise(expected, original)
    //    }
    //
    //    "be able to serialise a StringBigInt" in {
    //      val string = "10000000000000000000000000001"
    //      val original = StringBigInt(string)
    //      val expected:DBObject = new BasicDBObject("StringBigInt", string)
    //      mustSerialise(original, expected)
    //    }
    //
    //    "be able to deserialise a StringBigInt" in {
    //      val string = "10000000000000000000000000001"
    //      val original = StringBigInt(string)
    //      val expected = new BasicDBObject("StringBigInt", string)
    //      mustDeserialise(expected, original)
    //    }

    "be able to serialise a StringBigDecimal" in {
      val string = "100000000000000.00000000000001"
      val original = StringBigDecimal(string)
      mustSerialise(original, string)
    }

    "be able to serialise a StringBigInt" in {
      val string = "10000000000000000000000000001"
      val original = StringBigInt(string)
      mustSerialise(original, string)
    }

    "be able to serialise an homogeneous List" in {
      val a1 = List("a", "b", "c")
      val dbList = new BasicDBList()
      dbList.addAll(a1)
      val expected = new BasicDBObject(Map("value" -> dbList))
      mustSerialise(Map("value" -> a1), expected)
    }

    "be able to deserialise an homogeneous List" in {
      val a1 = List("a", "b", "c")
      val dbList = new BasicDBList()
      dbList.addAll(a1)
      val original = dbList
      mustDeserialise(original, a1)
    }

    "be able to serialise a Map" in {
      val original = Map("key" -> "value")
      mustSerialise(original, new BasicDBObject(original))
    }

    "be able to deserialise a Map" in {
      val original = Map("key" -> "value")
      mustDeserialise(new BasicDBObject(original), original)
    }

    "be able to ONLY serialise a nested Map" in {
      //Caveat: If you want to test in isolation only the serialiser,
      //you can't simply create a new BasicDBObject out of the Scala
      //nested Map. You need to embed the nested Map into a BasicDBObject as well!
      val nested = Map("b" -> "c")
      val original = Map("a" -> nested)
      val expected = new BasicDBObject()
      expected.put("a", new BasicDBObject(nested))
      mustSerialise(original, expected)
    }

    "be able to serialise/deserialise a nested Map" in {
      val original = Map("a" -> Map("b" -> Map("c" -> "!")))
      mustSerialiseAndDeserialise(original)
    }

    "be able to serialise a Person" in {
      val original = Person("John", "Doe")
      val expected = new BasicDBObject(Map("name" -> "John", "surname" -> "Doe", "id" -> original.id))
      mustSerialise(original, expected)
    }

    "be able to deserialise a Person" in {
      mustSerialiseAndDeserialise(Person("John", "Doe"))
    }

    "be able to serialise/deserialise a Student" in {
      val uuid = UUID.randomUUID()
      val original = Student(101287
        ,uuid
        ,"Alfredo"
        ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
        ,Address("Foo Rd.", 91)
        ,graduated = false
      )
      mustSerialiseAndDeserialise(original)
    }
  }

}
