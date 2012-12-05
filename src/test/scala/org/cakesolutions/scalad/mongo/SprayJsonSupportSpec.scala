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
                   collegeUuid: UUID,
                   name: String,
                   parents: List[Person],
                   address: Address,
                   graduated: Boolean)

class SprayJsonSupportTest extends Specification
  with DefaultJsonProtocol
  with MongoCrudTestAccess with UuidMarshalling {

  implicit val JsObjectEntityFormatter = jsonFormat1(JsValueEntity)
  implicit val DoubleEntityFormatter = jsonFormat1(DoubleEntity)
  implicit val PersonFormatter = jsonFormat2(Person)
  implicit val AddressFormatter = jsonFormat2(Address)
  implicit val StudentFormatter = jsonFormat6(Student)
  implicit val StudentSerialiser = new SprayJsonSerialisation[Student]
  implicit val PersonSerialiser = new SprayJsonSerialisation[Person]

  implicit val StudentCollectionProvider = new IndexedCollectionProvider[Student] {
    override def getCollection = db.getCollection("student")
    override def uniqueFields = "{'id': 1}" :: Nil
  }

  implicit val PersonCollectionProvider = new IndexedCollectionProvider[Person] {
    override def getCollection = db.getCollection("person")
    override def uniqueFields = "{'id': 1}" :: Nil
  }

  def mustSerialize[T: JsonFormat](entity: T, expected: DBObject) {
      val serializer = new SprayJsonSerialisation[T]
      // HACK: DBObject does not guarantee that it implements equals
      // as a workaround, we compare the JSON output, but that is no
      // real way to compare things
//      util.JSON.serialize(serializer.serialize(entity)) must beEqualTo(util.JSON.serialize(expected))
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

  "SprayJsonSerialisation" should {
    sequential

    val crud = new MongoCrud

    //Beware: Spray-Json's parser expect you to use double quotes:
    //https://github.com/spray/spray-json/blob/master/src/test/scala/spray/json/JsonParserSpec.scala
    val jsonQuery = """{"id": 101287}"""
    val nestedJsonQuery = """{"address": {"road": "Foo Rd.", "number": 91}}"""
    val studUuid = UUID.randomUUID()
    val student = Student(101287
                          , studUuid
                          ,"Alfredo"
                          ,List(Person("John", "Doe"), Person("Mary", "Lamb"))
                          ,Address("Foo Rd.", 91)
                          ,graduated = false
                          )

    val studentUpdate = Student(101287
                                , studUuid
                                ,"Alfredo Di Napoli"
                                ,List(Person("Bar", "Bar"))
                                ,Address("Foo Rd.", 91)
                                ,graduated = true
                                )

    "ensure a Student is correctly persisted" in {
      crud.create(student).get must beEqualTo(student)
    }

    "ensure a Student is searchable by id" in {
      implicit val ReadByWord = new LongFieldQuery[Student]("id")
      crud.readFirst(101287L).get must beEqualTo(student)
    }

    "ensure a Student is searchable by name" in {
      implicit val ReadByWord = new StringFieldQuery[Student]("name")
      crud.readFirst("Alfredo").get must beEqualTo(student)
    }

    "ensure a Student is searchable by UUID" in {
      implicit val UuidSerialiser = new SprayJsonSerialisation[UUID]
      implicit val ReadByUuid = new SerializedFieldQueryBuilder[Student, UUID]("collegeUuid")

      crud.readUnique(studUuid).get must beEqualTo(student)
    }

    "ensure a Student is searchable by JSON query" in {
      import SprayJsonImplicits._
      crud.searchFirst[Student](jsonQuery).get must beEqualTo(student)
    }

    "ensure a Student is searchable by nested JSON query" in {
      import SprayJsonImplicits._

      //Here I had to specify the WHOLE hierarchy, otherwise simply with
      //{"address": {"number": 91}}, mongo is not able to retrieve any
      //result
      crud.searchFirst[Student](nestedJsonQuery).get must beEqualTo(student)
    }
  }
}
