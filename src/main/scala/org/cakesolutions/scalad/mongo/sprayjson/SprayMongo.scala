package org.cakesolutions.scalad.mongo.sprayjson

import org.cakesolutions.scalad.mongo._
import spray.json.{JsValue, JsObject, JsonFormat}
import akka.contrib.jul.JavaLogging
import com.mongodb.{DB, DBObject}
import spray.json.pimpAny
import scala.language.implicitConversions

trait Implicits extends SprayJsonConvertors {
  this: JavaLogging =>

  protected implicit def serialiser[T: JsonFormat] = new SprayJsonSerialisation[T]

  protected implicit def SprayJsonToDBObject(json: JsValue) = js2db(json).asInstanceOf[DBObject]

}

/** MongoDB format for indexing fields, e.g. {"key": 1} */
class SprayMongoCollection[T](db: DB,
                              name: String,
                              indexFields: List[JsObject] = Nil,
                              uniqueFields: List[JsObject] = Nil)
  extends CollectionProvider[T] with Implicits with JavaLogging {

  def getCollection = db.getCollection(name)

  if (IndexedCollectionProvider.privilegedIndexing(getCollection)) {
    log.debug("Ensuring indexes exist on " + getCollection)
    uniqueFields.foreach(field => getCollection.ensureIndex(field, null, true))
    indexFields.foreach(field => getCollection.ensureIndex(field, null, false))
  }
}

/** Forwards all requests to the ScalaD API, independent of the Java MongoDB API.
  * Not all MongoCrud operations are exposed, in an effort to encourage good practice
  * when using Spray JSON. For example, it is easier to use `findAndModify` with the
  * DSL than to define an abstract identity extractor.
  */
class SprayMongo extends Implicits with JavaLogging {

  private val scalad = new MongoCrud

  def create[T: CollectionProvider : JsonFormat](entity: T): Option[T] = scalad.create(entity)

  def searchFirst[T: CollectionProvider : JsonFormat](query: JsObject): Option[T] = scalad.searchFirst(query)

  def searchAll[T: CollectionProvider : JsonFormat](query: JsObject): ConsumerIterator[T] = scalad.searchAll(query)

  def findAndModify[T: CollectionProvider](query: JsObject, rule: JsObject) = scalad.findAndModify(query, rule)

  def findAndReplace[T: CollectionProvider : JsonFormat](query: JsObject, update: T) = scalad.findAndModify(query, update.toJson)

  def deleteFirst[T: CollectionProvider : JsonFormat](query: JsObject): Option[T] = {
    val result = implicitly[CollectionProvider[T]].getCollection.findAndRemove(query)
    if (result == null) None
    else Some(serialiser[T] deserialise result)
  }

  def aggregate[T: CollectionProvider](pipeline: JsObject*): List[JsValue] = {
    val bsons = pipeline.map {
      SprayJsonToDBObject _
    }
    scalad.aggregate(bsons: _*).map {
      obj2js _
    }
  }
}
