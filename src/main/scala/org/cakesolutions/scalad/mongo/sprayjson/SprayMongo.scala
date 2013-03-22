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
                              uniqueIndexes: JsObject*)
  extends CollectionProvider[T] with Implicits with JavaLogging {

  def getCollection = db.getCollection(name)

  if (IndexedCollectionProvider.privilegedIndexing(getCollection)) {
    log.debug("Ensuring indexes exist on " + getCollection)
    uniqueIndexes.foreach(field => getCollection.ensureIndex(field, null, true))
    indexes.foreach(field => getCollection.ensureIndex(field, null, false))
  }

  def indexes: List[JsObject] = Nil
}

/** Forwards all requests to the ScalaD API, independent of the Java MongoDB API.
  * Not all MongoCrud operations are exposed, in an effort to encourage good practice
  * when using Spray JSON. For example, it is easier to use `findAndModify` with the
  * DSL than to define an abstract identity extractor.
  */
class SprayMongo extends Implicits with JavaLogging {

  private val scalad = new MongoCrud

  def create[T: CollectionProvider : JsonFormat](entity: T): Option[T] = scalad.create(entity)

  def findOne[T: CollectionProvider : JsonFormat](query: JsObject): Option[T] = scalad.searchFirst(query)

  def find[T: CollectionProvider : JsonFormat](query: JsObject): ConsumerIterator[T] = scalad.searchAll(query)

  def findAndModify[T: CollectionProvider](query: JsObject, rule: JsObject) = scalad.findAndModify(query, rule)

  def findAndReplace[T: CollectionProvider : JsonFormat](query: JsObject, update: T) = scalad.findAndModify(query, update.toJson)

  def deleteOne[T: CollectionProvider : JsonFormat](query: JsObject): Option[T] = {
    val result = implicitly[CollectionProvider[T]].getCollection.findAndRemove(query)
    if (result == null) None
    else Some(serialiser[T] deserialise result)
  }

  def count[T: CollectionProvider : JsonFormat](query: JsObject): Long =
    implicitly[CollectionProvider[T]].getCollection.count(query)

  def count[T: CollectionProvider : JsonFormat](): Long =
    implicitly[CollectionProvider[T]].getCollection.count()

  // note, mongodb 2.3.x introduced a lot of fixes to the aggregation framework,
  // e.g. allowing for binary data to be included in pipelines.
  // https://github.com/janm399/scalad/issues/63
  def aggregate[T: CollectionProvider](pipeline: JsObject*): List[JsValue] = {
    val bsons = pipeline.map {
      SprayJsonToDBObject _
    }
    scalad.aggregate(bsons: _*).map {
      obj2js _
    }
  }
}
