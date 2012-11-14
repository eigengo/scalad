package org.cakesolutions.scalad.mongo

import com.mongodb._
import spray.json._
import scala.Some
import collection.mutable.ArrayBuffer
import concurrent.{ExecutionContext, Lock, Future}
import collection.mutable
import java.util.concurrent.atomic.AtomicBoolean
import annotation.tailrec

/**
 * These implicits make the MongoDB API nicer to use, for example by allowing
 * JSON search queries to be passed instead of `DBObject`s.
 */
object MongoImplicits {
  implicit var JSON2DBObject = (json: String) => util.JSON.parse(json).asInstanceOf[DBObject]
}

/**
 * Search returned too many results (would have been a memory hazard to proceed).
 */
case class TooManyResults(query: DBObject) extends Exception

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
 * Easy way to add unique indexes to a Mongo collection.
 */
trait IndexedCollectionProvider[T] extends CollectionProvider[T] {

  doIndex()

  def doIndex() {
    indexFields.foreach(field => getCollection.ensureIndex(new BasicDBObject(field, 1), null, true))
  }

  protected def indexFields: List[String]
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
class MongoCrud extends MongoCreate
with MongoSearch
with MongoUpdate
with MongoDelete
with MongoRead
with MongoFind

/*
 * Create entities in the database, requires `CollectionProvider` and `MongoSerializer`.
 */
trait MongoCreate {

  /**
   * Use unique indices in MongoDB to ensure that duplicate entries are not created
   * (`CollectionProvider` is a good place to do this).
   * @return the parameter, or `None` if not added.
   */
  def create[T: CollectionProvider : MongoSerializer](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val result = collection.insert(serialiser serialize entity).getLastError
    if (result.ok()) Some(entity)
    else None
  }
}

/**
 * An `Iterable` that provides a very clean interface to the
 * Producer / Consumer pattern.
 *
 * Both the `hasNext` and `next` methods of the `Iterator`
 * may block if the consumer catches up with the producer.
 *
 * If the client wishes to cancel iteration early, the
 * `stop` method may be called to free up resources. Some
 * implementations may introduce a timeout feature which
 * will automatically free or log unclosed resources on
 * idle activity.
 *
 * Functional purists may use this in their `Iteratees`
 * patterns.
 *
 *
 * It is a common misconception that `Iterator.hasNext` is
 * not allowed to block.
 * However, the API documentation does not preclude
 * blocking behaviour. Note that `Queue` implementations
 * return `false` once the consumer reaches the tail – a
 * fundamental difference opposite this trait.
 */
trait ConsumerIterable[T] extends Iterable[T] {

  /**
   * Instruct the backing implementation to truncate at its
   * earliest convenience and dispose of resources.
   */
  def stop()

  /**
   * Callback which limits the number of entities in each call
   * to no more than the given parameter.
   *
   * (Note that paging does imply anything on the buffering strategy,
   * which decides how many entries to store in memory from a search
   * on the database.)
   */
  def page(entries: Int)(f: List[T] => Unit): Unit = ???
}

/**
 * Implementation that uses a `Queue` to buffer the results
 * of an operation, blocking on `hasNext`. `next` will
 * not block if `hasNext` is `true`.
 */
class ProducerConsumerIterable[T] extends ConsumerIterable[T] {

  // ?? is there a more "Scala" way to use wait/notify
  private val blocker = new AnyRef
  // used in hasNext, notify on changes
  private val lock = new Lock // used to avoid a race condition on close

  private val queue = new mutable.SynchronizedQueue[T]
  private val stopSignal = new AtomicBoolean
  private val closed = new AtomicBoolean

  def push(el: T) {
    queue.enqueue(el)
    blocker.synchronized(blocker.notify())
  }

  def stopped() = stopSignal.get

  def close() {
    lock.acquire()
    closed.set(true)
    lock.release()
    blocker.synchronized(blocker.notify())
  }

  override def iterator = new Iterator[T] {
    @tailrec
    override def hasNext =
      if (!queue.isEmpty) true
      else if (closed.get) !queue.isEmpty // non-locking optimisation
      else {
        lock.acquire()
        if (closed.get) {
          // avoids race condition with 'close'
          lock.release()
          !queue.isEmpty
        } else {
          lock.release()
          blocker.synchronized(blocker.wait()) // will block until more is known
          hasNext
        }
      }

    override def next() = queue.dequeue()
  }

  override def stop() {
    stopSignal set true
    blocker.synchronized(blocker.notify())
  }
}

trait ResultSelector[T] {

  /**
   * Called periodically by the selective search.
   *
   * @return the trimmed results according to the implementation specific criteria.
   */
  def trim(results: List[T]): Iterable[T]
}


/*
 * Search using MongoDB `DBObject`s.
 *
 * Implicit conversions from JSON syntax or DSLs bring these methods within reach of
 * most users.
 */
trait MongoSearch {

  /**
   * @return the first result from the result of the query, or `None` if nothing found.
   */
  def searchFirst[T: CollectionProvider : MongoSerializer](query: DBObject): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val cursor = collection.find(query)
    try
      if (cursor.hasNext) Some(serialiser deserialize cursor.next())
      else None
    finally
      cursor.close()
  }

  /**
   * @return all results from the query.
   */
  def searchAll[T: CollectionProvider : MongoSerializer](query: DBObject): ConsumerIterable[T] = {
    val iterable = new ProducerConsumerIterable[T]

    import ExecutionContext.Implicits._
    Future {
      val collection = implicitly[CollectionProvider[T]].getCollection
      val serialiser = implicitly[MongoSerializer[T]]
      val cursor = collection find query

      try {
        while (!iterable.stopped && cursor.hasNext) {
          val found = serialiser deserialize cursor.next()
          iterable.push(found)
        }
      } finally {
        iterable.close()
        cursor.close()
      }
    }

    iterable
  }

  /**
   * @return the only found entry, or `None` if nothing found.
   * @throws TooManyResults if more than one result.
   */
  def searchUnique[T: CollectionProvider : MongoSerializer](query: DBObject): Option[T] = {
    val results = searchAll(query).toList // blocks
    if (results.isEmpty) None
    else if (results.tail.isEmpty) Some(results.head)
    else throw new TooManyResults(query)
  }
}

/**
 * `UPDATE` Operations.
 */
trait MongoUpdate {

  /**
   * Updates the first entry that matches the identity query.
   * @return the parameter or `None` if the entity was not found in the database.
   */
  def updateFirst[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndModify(id, serialiser serialize entity) != null) Some(entity)
    else None
  }
}

/**
 * `DELETE` Operations.
 */
trait MongoDelete {

  /**
   * @return `None` if the delete failed, otherwise the parameter.
   */
  def deleteFirst[T: CollectionProvider : IdentityQueryBuilder](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndRemove(id) != null) Some(entity)
    else None
  }

}

/**
 * Operations requiring `IdentityQueryBuilder` and create/search.
 */
trait MongoFind {
  this: MongoSearch =>

  /**
   * @return the found entity or `None` if the entity was not found in the database.
   * @throws TooManyResults if more than one result.
   */
  def findUnique[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): Option[T] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchUnique(id)
  }

  /**
   * @return the found entity or `None` if the entity was not found in the database.
   */
  def findFirst[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): Option[T] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchFirst(id)
  }
}

/**
 * Operations requiring `KeyQueryBuilder` and create/search.
 */
trait MongoRead {
  this: MongoSearch =>

  /**
   * @return the only entity matching the key-based search, or `None`.
   * @throws TooManyResults if more than one result.
   */
  def readUnique[T, K](key: K)(implicit keyBuilder: KeyQueryBuilder[T, K],
                               collectionProvider: CollectionProvider[T],
                               serialiser: MongoSerializer[T]): Option[T] = {
    val query = keyBuilder.createKeyQuery(key)
    searchUnique(query)
  }

  /**
   * @return the first entity matching the key-based search, or `None`.
   */
  def readFirst[T, K](key: K)(implicit keyBuilder: KeyQueryBuilder[T, K],
                              collectionProvider: CollectionProvider[T],
                              serialiser: MongoSerializer[T]): Option[T] = {
    val query = keyBuilder.createKeyQuery(key)
    searchFirst(query)
  }
}


/**
 * Allows client code to select results by trimming incremental
 * result sets.
 *
 * This avoids running into memory problems when a search may return
 * many results, but a selection criteria (e.g. "10 most recent") is
 * to be used.
 */
trait MongoSelectiveSearch {

  /**
   * @param selector called periodically to trim the results
   * @return all results from the query and passing the selection criteria.
   *         The return type is different to
   *         [[org.cakesolutions.scalad.mongo.MongoSearch.searchAll( )]]
   *         and this method blocks.
   */
  def searchAll[T: CollectionProvider : MongoSerializer](query: DBObject, selector: List[T] => List[T]): List[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerializer[T]]

    val cursor = collection.find(query)
    try {
      val hits = new ArrayBuffer[T]
      while (cursor.hasNext) {
        hits += serialiser deserialize cursor.next()
        if (hits.length % 100 == 0) {
          val trimmed = selector(hits.toList)
          hits.clear()
          hits ++= trimmed
        }
      }
      selector(hits.toList)
    } finally
      cursor.close()
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
  implicit def sprayJsonStringSerializer[T: JsonFormat]: MongoSerializer[T] = new SprayJsonStringSerialisation[T]
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
 * Provides a `read` query that resembles SQL's ``SELECT a WHERE a.field = ...``.
 *
 * The key must not require any special serialisation.
 */
trait FieldQueryBuilder[T, K] extends KeyQueryBuilder[T, K] {
  def createKeyQuery(key: K): DBObject = new BasicDBObject(field, key)

  def field: String
}

/**
 * Provides a concept of identity that resembles a SQL `field` column
 *
 * The key must not require any special serialisation.
 */
trait FieldIdentityQueryBuilder[T, K] extends IdentityQueryBuilder[T] {
  def createIdQuery(entity: T): DBObject = new BasicDBObject(field, id(entity))

  def field: String

  def id(entity: T): K
}

/**
 * Syntactic sugar for [[org.cakesolutions.scalad.mongo.FieldQueryBuilder]]
 */
class FieldQuery[T, K](val field: String) extends FieldQueryBuilder[T, K]

/**
 * Conveniently mixes together two traits that are often seen together.
 */
trait IdentityField[T, K] extends FieldQueryBuilder[T, K] with FieldIdentityQueryBuilder[T, K]

/**
 * Solidifies [[org.cakesolutions.scalad.mongo.IdentityField]]
 * for fields named `id`.
 */
trait IdField[T <: {def id : K}, K] {
  def id(entity: T) = entity.id

  def field = "id"
}

/**
 * Convenient mixin to provide
 * [[org.cakesolutions.scalad.mongo.IdField]]
 * support for simple field types.
 */
trait ImplicitIdField[T <: {def id : K}, K] {
  implicit val IdField = new IdentityField[T, K] with IdField[T, K]
}


/**
 * Specialisation of [[org.cakesolutions.scalad.mongo.FieldQueryBuilder]]
 * for fields that are serialized to `String` but interpreted as a special case
 * by `BasicDBObject`, i.e. UUID.
 */
trait FieldAsString[T, K] extends FieldQueryBuilder[T, K] {
  override def createKeyQuery(key: K): DBObject = new BasicDBObject(field, key.toString)
}

/**
 * Friend of [[org.cakesolutions.scalad.mongo.FieldAsString]].
 */
trait IdentityAsString[T, K] extends FieldIdentityQueryBuilder[T, K] {
  override def createIdQuery(entity: T): DBObject = new BasicDBObject(field, id(entity).toString)
}

/**
 * Conveniently mixes together two traits that are often seen together.
 */
trait IdentityFieldAsString[T, K] extends FieldAsString[T, K] with IdentityAsString[T, K]


/**
 * Provides a `read` query using serialised fields.
 */
class SerializedFieldQueryBuilder[T, K](val field: String)
                                       (implicit serialiser: MongoSerializer[K])
  extends FieldQueryBuilder[T, K] {
  override def createKeyQuery(key: K): DBObject = new BasicDBObject(field, serialiser.serialize(key))
}

/**
 * Provides a concept of identity that resembles a SQL `field` column,
 * with serialization on the field.
 */
abstract class SerializedIdentityQueryBuilder[T, K](val field: String)
                                                   (implicit serialiser: MongoSerializer[K])
  extends FieldIdentityQueryBuilder[T, K] {
  override def createIdQuery(entity: T) = new BasicDBObject(field, serialiser.serialize(id(entity)))
}
