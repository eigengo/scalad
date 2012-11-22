package org.cakesolutions.scalad.mongo

import com.mongodb._
import java.util.UUID
import org.scalacheck.Properties
import org.scalacheck.Prop._
import org.specs2.mutable.Specification
import scala.math.BigInt.probablePrime
import scala.util.Random
import spray.json._

trait MongoCrudTestAccess {
  val m = new Mongo()
  m.setWriteConcern(WriteConcern.SAFE)
  // write concern needed to catch constraint violations (Mongo Magic)
  val db = m.getDB("MongoCrudTest")
  db.dropDatabase()
}

case class LongEntity(id: Long, word: String)

trait LongEntityPersistence extends MongoCrudTestAccess with DefaultJsonProtocol with ImplicitIdField[LongEntity, Long] {

  implicit val LongEntityCollectionProvider = new IndexedCollectionProvider[LongEntity] {
    override def getCollection = db.getCollection("long")

    override def uniqueFields = "{'id': 1}" :: Nil
  }

  // this would usually be defined elsewhere, e.g. the global Marshalling definitions
  implicit val LongEntityFormatter = jsonFormat2(LongEntity)

  // see UuidEntityPersistence for an alternative way to get serialisation
  implicit val LongEntitySerialiser = new SprayJsonStringSerialisation[LongEntity]

  // create a non-identity search
  // NOTE: implicitly chosen by type, so multiple fields of the same type
  // will mean the client has to explicitly list the search builders.
  // it is therefore best practice to create custom case classes for
  // all fields – it will also give additional type safety elsewhere.
  implicit val ReadByWord = new FieldQuery[LongEntity, String]("word")

}

case class SimpleEntity(word: String, another: String)

case class UuidEntity(id: UUID, simple: SimpleEntity)

trait UuidEntityMarshalling extends DefaultJsonProtocol with UuidMarshalling {
  implicit val SimpleEntityFormatter = jsonFormat2(SimpleEntity)
  implicit val UuidEntityFormatter = jsonFormat2(UuidEntity)
}

trait UuidEntityPersistence extends MongoCrudTestAccess with UuidEntityMarshalling with SprayJsonStringSerializers {

  implicit val UuidEntityCollectionProvider = new IndexedCollectionProvider[UuidEntity] {
    override def getCollection = db.getCollection("uuid")

    override def uniqueFields = "{'id': 1}" :: Nil
  }

  implicit val UuidIdAsString = new IdentityFieldAsString[UuidEntity, UUID] with IdField[UuidEntity, UUID]

  implicit val ReadBySimple = new SerializedFieldQueryBuilder[UuidEntity, SimpleEntity]("simple")
}

object UuidEntityPersistence {
  def newRandom() = {
    new UuidEntity(UUID.randomUUID(), new SimpleEntity(randomString(), randomString()))
  }

  //Idea taken from "Scala for the impatient", chapter 1, exercise 8
  def randomString() =  probablePrime(100, Random).toString(36) take 16
  
}

/**
 * MongoDB *must* be running locally.
 *
 * Start mongodb with `mongod --dbpath mongodb` (after creating the dir).
 */
class MongoCrudTest extends Specification with LongEntityPersistence with UuidEntityPersistence {
  sequential

  "Spray String JSON serialisation for Long ids" should {
    sequential

    val crud = new MongoCrud
    val long = 13L
    val jsonQuery = "{'id': 13}"
    val entity = LongEntity(long, "original")
    val update = LongEntity(long, "update")

    "return self from create()" in {
      crud.create(entity).get mustEqual (entity)
    }

    "throw MongoException when create() violates constraint" in {
      crud.create(entity) must throwA[MongoException]
    }

    "be searchable by field" in {
      crud.readFirst("original").get mustEqual (entity)
    }

    "be searchable by identity field" in {
      crud.readUnique(long).get mustEqual (entity)
    }

    "be searchable by JSON query" in {
      import Implicits._
      crud.searchFirst[LongEntity](jsonQuery).get mustEqual (entity)
    }

    "be searchable by example" in {
      crud.findUnique(entity).get mustEqual (entity)
    }

    "be updatable by example" in {
      crud.updateFirst(update).get mustEqual (update)
    }

    "be searchable with restrictions" in {
      failure
    }

    "be pageable in searches" in {

      //      crud.searchAll[LongEntity](jsonQuery).page(10){e => doStuff(e)}
      failure
    }

    "be stress tested in situations that use the ConsumerIterable" in {
      // not implemented yet
      failure
    }
  }

  "Spray String JSON serialisation for String UUID ids" should {
    sequential

    val crud = new MongoCrud
    val uuid = UUID.randomUUID()
    val entity = UuidEntity(uuid, SimpleEntity("original", "foo"))
    val update = UuidEntity(uuid, SimpleEntity("update", "bar"))

    "return self from create()" in {
      crud.create(entity).get mustEqual (entity)
    }

    "throw MongoException when create() violates constraint" in {
      crud.create(entity) must throwA[MongoException]
    }

    "be searchable by field" in {
      crud.readFirst(SimpleEntity("original", "foo")).get mustEqual (entity)
    }

    "be searchable by identity field" in {
      crud.readUnique(uuid).get mustEqual (entity)
    }

    // TODO: understand why this test fails.
    // It fails because, as weird as it may sound, you can't simply
    // nest json object. You need to qualified the field of the object
    // you need to find. Take a look here:
    //http://stackoverflow.com/questions/8983482/mongodb-finding-nested-elements
    "be searchable by JSON query" in {
      import Implicits._
      crud.searchFirst[UuidEntity]( """{"simple.word": "original"}""").get mustEqual (entity)
    }

    "be searchable by example" in {
      crud.findUnique(entity).get mustEqual (entity)
    }

    "be updatable by example" in {
      crud.updateFirst(update).get mustEqual (update)
    }
  }
}

//These tests are bogus one to explore ScalaCheck. Please ignore them for now.
object StringSpec extends Properties("String") {
  property("startsWith") = forAll((a: String, b: String) => (a+b).startsWith(a))

  property("concatenate") = forAll((a: String, b: String) =>
      (a+b).length > a.length && (a+b).length > b.length
  )

  property("substring") = forAll((a: String, b: String, c: String) =>
      (a+b+c).substring(a.length, a.length+b.length) == b
  )
}
