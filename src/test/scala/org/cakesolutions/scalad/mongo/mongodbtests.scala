package org.cakesolutions.scalad.mongo

import org.specs2.mutable.Specification
import com.mongodb._
import scala.util.Random
import java.util.UUID
import spray.json.{JsValue, JsString, JsonFormat, DefaultJsonProtocol}

trait MongoCrudTestAccess {
  val m = new Mongo()
  m.setWriteConcern(WriteConcern.SAFE)
  // write concern needed to catch constraint violations (Mongo Magic)
  val db = m.getDB("MongoCrudTest")
  db.dropDatabase()
}

trait UuidMarshalling {
  implicit object UuidJsonFormat extends JsonFormat[UUID] {
    def write(x: UUID) = JsString(x toString ())
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => sys.error("Expected UUID as JsString, but got " + x)
    }
  }
}

case class LongEntity(id: Long, word: String)

trait LongEntityPersistence extends MongoCrudTestAccess with DefaultJsonProtocol with IdField[LongEntity, Long] {

  implicit val LongEntityCollectionProvider = new CollectionProvider[LongEntity] with UniqueIndex[LongEntity] {
    override def getCollection = db.getCollection("long")

    override def indexFields = List("id")
  }

  // this would usually be defined elsewhere, e.g. the global Marshalling definitions
  implicit val LongEntityFormatter = jsonFormat2(LongEntity)

  // see UuidEntityPersistence for an alternative way to get serialisation
  implicit val LongEntitySerialiser = new SprayJsonStringSerialisation[LongEntity]
}

case class SimpleEntity(word: String, another: String)

case class UuidEntity(id: UUID, simple: SimpleEntity)

trait UuidEntityMarshalling extends DefaultJsonProtocol with UuidMarshalling {
  implicit val SimpleEntityFormatter = jsonFormat2(SimpleEntity)
  implicit val UuidEntityFormatter = jsonFormat2(UuidEntity)
}

trait UuidEntityPersistence extends MongoCrudTestAccess with StringIdField[UuidEntity, UUID]
  with UuidEntityMarshalling with SprayJsonStringSerializers {

  implicit val UuidEntityCollectionProvider = new CollectionProvider[UuidEntity] with UniqueIndex[UuidEntity] {
    override def getCollection = db.getCollection("uuid")

    override def indexFields = List("id")
  }
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
    val long = Random.nextLong()
    val entity = new LongEntity(long, "original")
    val update = new LongEntity(long, "update")

    "return self from create()" in {
      crud.create(entity).unsafePerformIO().get mustEqual (entity)
    }

    "throw MongoException when create() violates constraint" in {
      crud.create(entity) must throwA[MongoException]
    }

    "be searchable by field" in {
      crud.readUnique(long).unsafePerformIO().get mustEqual (entity)
    }

    "be searchable by example" in {
      crud.findUnique(entity).unsafePerformIO().get mustEqual (entity)
    }

    "be updatable by example" in {
      crud.updateFirst(update).unsafePerformIO().get mustEqual (update)
    }
  }

  "Spray String JSON serialisation for String UUID ids" should {
    sequential

    val crud = new MongoCrud
    val uuid = UUID.randomUUID()
    val entity = new UuidEntity(uuid, new SimpleEntity("original", "foo"))
    val update = new UuidEntity(uuid, new SimpleEntity("update", "bar"))

    "return self from create()" in {
      crud.create(entity).unsafePerformIO().get mustEqual (entity)
    }

    "throw MongoException when create() violates constraint" in {
      crud.create(entity) must throwA[MongoException]
    }

    "be searchable by field" in {
      crud.readUnique(uuid).unsafePerformIO().get mustEqual (entity)
    }

    "be searchable by example" in {
      crud.findUnique(entity).unsafePerformIO().get mustEqual (entity)
    }

    "be updatable by example" in {
      crud.updateFirst(update).unsafePerformIO().get mustEqual (update)
    }
  }
}
