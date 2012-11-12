package org.cakesolutions.scalad.mongo

import com.mongodb._
import spray.json._
import scala.Some
import collection.mutable.ArrayBuffer
import scalaz.effect.IO

/**
 * Search returned too many results (would have been a memory hazard to proceed).
 */
case class TooManyResults(query: DBObject, results: Int) extends Exception

/**
 * Mechanism for finding an entry in the database
 * which matches a query built from an archetype entity.
 */
trait IdentityQueryBuilder[T] {
  def createIdQuery(entity: T): DBObject
}

/**
 * Mechanism for finding an entry in the database
 * which matches the query built up from a key.
 */
trait KeyQueryBuilder[T, K] {
  def createKeyQuery(key: K): DBObject
}

/**
 * Mechanism for converting to/from Scala types and MongoDB `DBObject`s.
 */
trait MongoSerializer[T] {
  def serialize(entity: T): DBObject

  def deserialize(dbObject: DBObject): T
}

/**
 * Access to a MongoDB `DBCollection`.
 * Here is a good place to add an index.
 */
trait CollectionProvider[T] {

  def getCollection: DBCollection
}

/**
 * Easy way to add a unique index onto a Mongo collection.
 */
trait UniqueIndex[T] {
  this: CollectionProvider[T] =>

  indexFields.foreach {
    field => getCollection.ensureIndex(new BasicDBObject(field, 1), null, true)
  }

  def indexFields: List[String]
}

/**
 * Provides CRUD access to a MongoDB collection using client-provided implicits to:
 *
 * 1. provide the backing MongoDB `DBCollection`.
 * 2. serialise/deserialise the MongoDB representation.
 * 3. provide a concept of identity for UPDATE/DELETE operations.
 * 4. provide a concept of a key for READ operations.
 *
 * MongoDB adds an internal `_id` field to every object that is persisted in the
 * database. We simply ignore that field in Scala land.
 *
 * ALl methods throw [[com.mongodb.MongoException]] if something bad happened that we
 * didn't expect (e.g. I/O or config).
 *
 * @author Sam Halliday
 * @author Jan Machacek
 * @see <a href="http://www.cakesolutions.net/teamblogs/2012/11/05/crud-options/">Thinking notes on the API design</a>
 */
class MongoCrud extends MongoCreateAndSearch with MongoUpdateAndDelete with MongoRead with MongoFind

/*
 * Operations that only require `CollectionProvider` and `MongoSerializer`.
 */
trait MongoCreateAndSearch {

  def MaxResults = 10000

  /**
   * Use unique indices in MongoDB to ensure that duplicate entries are not created
   * (`CollectionProvider` is a good place to do this).
   * @return the parameter, or `None` if not added.
   */
  def create[T: CollectionProvider : MongoSerializer](entity: T): IO[Option[T]] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val result = collection.insert(serialiser serialize entity).getLastError
    if (result.ok()) IO(Some(entity))
    else IO(None)
  }

  /**
   * @return the first result from the result of the query, or `None` if nothing found.
   */
  def searchFirst[T: CollectionProvider : MongoSerializer](query: DBObject): IO[Option[T]] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val cursor = collection.find(query)
    try
      if (cursor.hasNext) IO(Some(serialiser deserialize cursor.next()))
      else IO(None)
    finally
      cursor.close()
  }

  /**
   * @return all results from the query.
   * @throws TooManyResults if there were too many results.
   */
  def searchAll[T: CollectionProvider : MongoSerializer](query: DBObject): IO[IndexedSeq[T]] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val cursor = collection.find(query)
    try {
      val hits = new ArrayBuffer[T]
      while (cursor.hasNext) {
        hits += serialiser deserialize cursor.next()
        if (hits.length > MaxResults)
          throw new TooManyResults(query, hits.length)
      }
      IO(hits.toIndexedSeq)
    } finally
      cursor.close()
  }

  // TODO: searchTop which returns maximum of MaxResults and requires rejection policy
  // (would be very useful for version fields)

  /**
   * @return the only found entry, or `None` if nothing found.
   * @throws TooManyResults if more than one result.
   */
  def searchUnique[T: CollectionProvider : MongoSerializer](query: DBObject): IO[Option[T]]= {
    import scalaz.syntax.monad._

    def unique(results: Seq[T]): Option[T] = {
      if (results.isEmpty) None
      else if (results.tail.isEmpty) Some(results.head)
      else throw new TooManyResults(query, results.length)
    }

    searchAll(query) >>= {r => IO(unique(r)) }
  }
}

/**
 * Operations requiring an `IdentityQueryBuilder`.
 */
trait MongoUpdateAndDelete {

  /**
   * Updates the first entry that matches the identity query.
   * @return the parameter or `None` if the entity was not found in the database.
   */
  def updateFirst[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): IO[Option[T]] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndModify(id, serialiser serialize entity) != null) IO(Some(entity))
    else IO(None)
  }

  /**
   * @return `None` if the delete failed, otherwise the parameter.
   */
  def deleteFirst[T: CollectionProvider : IdentityQueryBuilder](entity: T): IO[Option[T]] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndRemove(id) != null) IO(Some(entity))
    else IO(None)
  }
}

/**
 * Operations requiring `IdentityQueryBuilder` and create/search.
 */
trait MongoFind {
  this: MongoCreateAndSearch =>

  /**
   * @return the found entity or `None` if the entity was not found in the database.
   * @throws TooManyResults if more than one result.
   */
  def findUnique[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): IO[Option[T]] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchUnique(id)
  }

  /**
   * @return the found entity or `None` if the entity was not found in the database.
   */
  def findFirst[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): IO[Option[T]] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchFirst(id)
  }
}

/**
 * Operations requiring `KeyQueryBuilder` and create/search.
 */
trait MongoRead {
  this: MongoCreateAndSearch =>

  /**
   * @return the only entity matching the key-based search, or `None`.
   * @throws TooManyResults if more than one result.
   */
  def readUnique[K, T](key: K)(implicit keyBuilder: KeyQueryBuilder[T, K],
                         collectionProvider: CollectionProvider[T],
                         serialiser: MongoSerializer[T]): IO[Option[T]] = {
    val query = keyBuilder.createKeyQuery(key)
    searchUnique(query)
  }

  /**
   * @return the first entity matching the key-based search, or `None`.
   */
  def readFirst[K, T](key: K)(implicit keyBuilder: KeyQueryBuilder[T, K],
                              collectionProvider: CollectionProvider[T],
                              serialiser: MongoSerializer[T]): IO[Option[T]] = {
    val query = keyBuilder.createKeyQuery(key)
    searchFirst(query)
  }
}

/**
 * Uses `spray-json` to serialise/deserialise database objects via
 * an intermediary `String` stage to force a JSON representation of
 * the objects.
 *
 * Note: `UUID` objects are persisted as `String`s not BSON.
 */
class SprayJsonStringSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  override def serialize(entity: T): DBObject = {
    val formatter = implicitly[JsonFormat[T]]
    val json = formatter.write(entity).compactPrint
    util.JSON.parse(json).asInstanceOf[DBObject] // see JSON.parse and cry – this works for well formed JSON
  }

  override def deserialize(found: DBObject): T = {
    if (found.containsField("_id")) // internal MongoDB field
      found.removeField("_id")

    val json = util.JSON.serialize(found)
    val parsed = JsonParser.apply(json)
    val formatter = implicitly[JsonFormat[T]]
    formatter.read(parsed)
  }
}

/**
 * Mix this in to automatically get an implicit SprayJsonStringSerialisation in your scope
 */
trait SprayJsonStringSerializers {

  /**
   * Construct MongoSerializer for A, if there is an instance of JsonWriter for A
   */
  implicit def sprayJsonStringSerializer[T : JsonFormat]: MongoSerializer[T] = new SprayJsonStringSerialisation[T]
}

/**
 * Uses `spray-json` to serialise/deserialise database objects
 * directly from `JsObject` -> `DBObject`.
 *
 * Note: `UUID` objects are persisted as `String`s not BSON.
 */
class SprayJsonSerialisation[T: JsonFormat] extends MongoSerializer[T] {

  override def serialize(entity: T): DBObject = {
    val formatter = implicitly[JsonFormat[T]]
    val jsObject = formatter.write(entity).asJsObject
    ???
  }

  override def deserialize(found: DBObject): T = {
    val jsObject = ???
    val formatter = implicitly[JsonFormat[T]]
    formatter.read(jsObject)
  }
}


/**
 * Provides a concept of identity that resembles a SQL `id` column
 * and
 * provides a `read` query that resembles SQL's ``SELECT a WHERE a.id = ...``.
 */
class SingleFieldId[T <: {def id : K}, K](val field: String) extends IdentityQueryBuilder[T] with KeyQueryBuilder[T, K] {

  def createIdQuery(entity: T) = createFieldIdQuery(entity.id)

  def createKeyQuery(key: K) = createFieldIdQuery(key)

  protected def createFieldIdQuery(id: K): DBObject = new BasicDBObject(field, id)
}

/**
 * Mixin to get support for SQL style `id` columns
 */
trait IdField[T <: {def id : K}, K] {
  implicit val IdField = new SingleFieldId[T, K]("id")
}

/**
 * Specialisation of [[org.cakesolutions.scalad.mongo.SingleFieldId]]
 * for `id`s that are stored as `String` but not interpreted as `String` by `DBObject`.
 *
 * This is basically a workaround for `UUID` as many serialisers will save as `String`,
 * but direct `DBObject` construction creates a special BSON object.
 */
class SingleFieldAsStringId[T <: {def id : K}, K](field: String) extends SingleFieldId[T, K](field) {

  override protected def createFieldIdQuery(id: K) = new BasicDBObject(field, id.toString)
}

/**
 * Mixin to get support for SQL style `id` columns that need the
 * [[org.cakesolutions.scalad.mongo.SingleFieldAsStringId]]
 * workaround.
 */
trait StringIdField[T <: {def id : K}, K] {
  implicit val StringIdField = new SingleFieldAsStringId[T, K]("id")
}
