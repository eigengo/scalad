package org.cakesolutions.scalad.mongo

import java.util.UUID
import collection.mutable.ListBuffer
import com.mongodb.{BasicDBObjectBuilder, DBObject}
import util.Random

trait HandcraftedPersistence extends MongoCrudTestAccess with ImplicitIdField[UuidEntity, UUID] {

  implicit val UuidEntityCollectionProvider = new IndexedCollectionProvider[UuidEntity] {
    override def getCollection = db.getCollection("uuid_hand")

    override def indexFields = "{id: 1}" :: Nil
  }

  implicit val DirectSerialisation = new MongoSerializer[UuidEntity] {
    def serialize(entity: UuidEntity) = {
      BasicDBObjectBuilder.start()
        .add("id", entity.id)
        .add("simple", BasicDBObjectBuilder.start()
        .add("word", entity.simple.word)
        .add("another", entity.simple.another)
        .get())
        .get()
    }

    def deserialize(dbObject: DBObject) = {
      val uuid = dbObject.get("id").asInstanceOf[UUID]
      val simple = dbObject.get("simple").asInstanceOf[DBObject]
      val word = simple.get("word").asInstanceOf[String]
      val another = simple.get("another").asInstanceOf[String]
      UuidEntity(uuid, SimpleEntity(word, another))
    }
  }
}

/**
 * Does a speed comparison of serialisation of the test entities (from mongodbtests)
 * using various marshallers Vs direct DBObject construction.
 *
 * To change the serialisation, change the mixin.
 *
 * Conclusions:
 *
 * 1,000 DB entries:
 *
 * 1. JSON String: WRITE ~ 0.17ms, READ 0.22ms (without index ~ 0.60ms)
 * 2. Direct: WRITE ~ 0.16ms, READ 0.15ms (without index ~ 0.54ms)
 *
 *
 * 10,000 DB entries:
 *
 * 1. JSON String: WRITE ~ 0.17ms, READ ~0.23ms
 * 2. Direct: WRITE ~ 0.17ms, READ ~ 0.16ms
 *
 *
 * 100,000 DB entries:
 *
 * 1. JSON String: WRITE ~ 0.17ms, READ ~ 0.23ms
 * 2. Direct: WRITE ~ 0.17ms, READ ~ 0.15ms
 *
 *
 * 1,000,000 DB entries:
 *
 * 1. JSON String: WRITE ~ 0.22ms, READ ~ 0.23ms
 * 2. Direct: WRITE ~ 0.20s, READ ~ 0.16ms
 */
object mongodbprofiling
extends HandcraftedPersistence
//  extends UuidEntityPersistence
{

  def main(args: Array[String]) {
    val count = 1000
    val runs = 10
    val repeats = 3
    val dataBuffer = new ListBuffer[UuidEntity]
    for (i <- 0 until count) {
      dataBuffer append UuidEntityPersistence.newRandom()
    }
    val data = dataBuffer.toList
    println("This many: " + data.length)

    val crud = new MongoCrud

    println("WRITE")
    for (i <- 1 to repeats) {
      val start = System.currentTimeMillis()
      for (i <- 1 to runs) {
        val provider = implicitly[IndexedCollectionProvider[UuidEntity]]
        provider.getCollection.drop()
        provider.doIndex()
        data.foreach(datum => crud.create(datum))
      }
      val end = System.currentTimeMillis()

      println((end - start) / (1000.0 * runs * data.length))
    }

    // database populated at this stage
    println("READ")
    for (i <- 1 to repeats) {
      val shuffled = Random.shuffle(data)

      val start = System.currentTimeMillis()
      for (i <- 1 to runs) {
        shuffled.forall(datum => crud.readFirst(datum.id).get == datum)
      }
      val end = System.currentTimeMillis()

      println((end - start) / (1000.0 * runs * data.length))
    }

  }
}
