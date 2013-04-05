package org.eigengo.scalad.mongo

import com.mongodb._
import org.specs2.mutable.Specification
import java.util.concurrent.atomic.AtomicInteger

trait MongoCrudTestAccess {
  val m = new Mongo()
  m.setWriteConcern(WriteConcern.SAFE)
  // write concern needed to catch constraint violations (Mongo Magic)
  val db = m.getDB("MongoCrudTest")
  db.dropDatabase()
}

case class LongEntity(id: Long, word: String)

trait LongEntityPersistence extends MongoCrudTestAccess {

  implicit val LongEntitySerialiser = new MongoSerialiser[LongEntity] {
    def serialise(entity: LongEntity) = new BasicDBObjectBuilder().append("id", entity.id).append("word", entity.word).get()

    def deserialise(dbObject: Object) = {
      val o = dbObject.asInstanceOf[BasicDBObject]
      LongEntity(o.getLong("id"), o.getString("word"))
    }
  }
  implicit val LongEntityCollectionProvider = new IndexedCollectionProvider[LongEntity] {
    def getCollection = db.getCollection("long_entities")
  }
  implicit val LongEntityLongKey = new LongFieldQuery[LongEntity]("id")
  implicit val LongEntityIdentity = new FieldIdentityQueryBuilder[LongEntity, Long]{
    def field = "id"
    def id(entity: LongEntity) = entity.id
  }
  implicit val StringSerialiser = new MongoSerialiser[String] {
    def serialise(entity: String) = entity

    def deserialise(dbObject: Object) = dbObject.toString
  }
  implicit val ReadByWord = new SerialisedFieldQueryBuilder[LongEntity, String]("word")
}

/**
 * MongoDB *must* be running locally.
 *
 * Start mongodb with `mongod --dbpath mongodb` (after creating the dir).
 */
class MongoCrudTest extends Specification with LongEntityPersistence {
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
      todo
    }

    "be pageable in searches" in {
      val counter = new AtomicInteger()
      import Implicits._
      crud.searchAll[LongEntity](jsonQuery).page(10) {
        e => counter.addAndGet(1)
      }
      counter.get mustEqual 1
    }

    "be stress tested in situations that use the ConsumerIterable" in {
      // not implemented yet
      todo
    }
  }

}
