package org.cakesolutions.scalad.mongo.sprayjson

import spray.json._
import com.mongodb._
import org.bson.types._
import java.util.{Date, UUID}
import java.text.{ParseException, SimpleDateFormat}
import java.net.URI
import scala.Some
import util.JSON
import org.cakesolutions.scalad.mongo.{IdentityQueryBuilder, KeyQueryBuilder, IndexedCollectionProvider}
import akka.contrib.jul.JavaLogging


/** Convenience that allows a collection to be setup as using Spray JSON marshalling */
trait IndexedCollectionSprayJson[T] extends SprayJsonSerialisation[T] with IndexedCollectionProvider[T]

/** Convenience that supports a simple ("id" field) type, using Spray JSON marshalling. */
class SimpleSprayJsonCollection[T <: {def id : K}, K]
  (db: DB, collection: String)
  (implicit sprayT: JsonFormat[T], idSerialiser: SprayJsonSerialisation[K], sprayK: JsonFormat[K])
  extends IndexedCollectionSprayJson[T]
  with KeyQueryBuilder[T, K] with IdentityQueryBuilder[T] {

  def getCollection = db.getCollection(collection)

  override protected def uniqueFields = """{"id": 1}""" :: Nil

  override def createKeyQuery(key: K): DBObject = new BasicDBObject("id", idSerialiser.serialize(key))

  import language.reflectiveCalls
  override def createIdQuery(entity: T): DBObject = new BasicDBObject("id", idSerialiser.serialize(entity.id))

}

/** Mixin to get an implicit SprayJsonSerialisation in scope. */
trait SprayJsonSerializers {
  implicit def sprayJsonSerializer[T: JsonFormat]: SprayJsonSerialisation[T] = new SprayJsonSerialisation[T]
}


/** Pimp and implicits that gives conversion from String/JsValue to DBObject
  * using the implicit serialisers.
  *
  * Note: Spray JSON is used to do the parsing, so JSON has to be strictly valid.
  * (Mongo's interpretation of JSON is a little loose).
  */
object MongoQueries extends MongoQueries with JavaLogging

trait MongoQueries extends SprayJsonConvertors {
  this: JavaLogging =>

  import scala.language.implicitConversions

  implicit def SprayJsonToDBObject(json: JsValue) = js2db(json).asInstanceOf[DBObject]

  implicit class MongoString(query: String) {

    private def stringToMongo(query: String) = SprayJsonToDBObject(JsonParser(query))

    def toBson = stringToMongo(query)

    // we have to do each parameter list explicitly, without * syntax,
    // because the compiler needs to be able to get each serialiser.
    def param[T](q1: T)(implicit q1s: JsonWriter[T]) = {
      stringToMongo(query format(q1.toJson))
    }

    def params[T, U](q1: T, q2: U)(implicit q1s: JsonWriter[T], q2s: JsonWriter[U]) = {
      stringToMongo(query format(q1.toJson, q2.toJson))
    }

    def params[T, U, V](q1: T, q2: U, q3: V)
                      (implicit q1s: JsonWriter[T], q2s: JsonWriter[U], q3s: JsonWriter[V]) = {
      stringToMongo(query format(q1.toJson, q2.toJson, q3.toJson))
    }

    def params[T, U, V, W](q1: T, q2: U, q3: V, q4: W)
                         (implicit q1s: JsonWriter[T], q2s: JsonWriter[U], q3s: JsonWriter[V], q4s: JsonWriter[W]) = {
      stringToMongo(query format(q1.toJson, q2.toJson, q3.toJson, q4.toJson))
    }
  }

}





