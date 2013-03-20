package org.cakesolutions.scalad.mongo

import com.mongodb.DBObject
import concurrent.{ExecutionContext, Future}
import akka.contrib.jul.JavaLogging

/** Search returned too many results.
  */
case class TooManyResults(query: DBObject) extends Exception

/** Search using MongoDB `DBObject`s.
  *
  * `searchAll` returns immediately and builds up the results into an `Iterable`
  * as they are found.
  *
  * Implicit conversions from JSON syntax or DSLs bring these methods within reach of
  * most users.
  */
trait MongoSearch extends JavaLogging {

  /** @return the first result from the result of the query, or `None` if nothing found. */
  def searchFirst[T: CollectionProvider : MongoSerialiser](query: DBObject): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerialiser[T]]

    val cursor = collection.find(query)
    try
      if (cursor.hasNext) Some(serialiser deserialise cursor.next())
      else None
    finally
      cursor.close()
  }

  /** @return all results from the query. */
  def searchAll[T: CollectionProvider : MongoSerialiser](query: DBObject): ConsumerIterator[T] = {
//    val iterable = new NonblockingProducerConsumer[T]
    val iterable = new BlockingProducerConsumer[T](100)

    import ExecutionContext.Implicits.global
    Future {
      val collection = implicitly[CollectionProvider[T]].getCollection
      val serialiser = implicitly[MongoSerialiser[T]]
      val cursor = collection find query

      try {
        while (!iterable.stopped && cursor.hasNext) {
          val found = serialiser deserialise cursor.next()
          iterable.produce(found)
        }
      } finally {
        iterable.close()
        cursor.close()
      }
    }.onFailure {
      case t => log.error(t, "Future failed")
    }

    iterable
  }

  /** @return the only found entry, or `None` if nothing found.
    * @throws TooManyResults if more than one result.
    */
  def searchUnique[T: CollectionProvider : MongoSerialiser](query: DBObject): Option[T] = {
    val results = searchAll(query).toList // blocks
    if (results.isEmpty) None
    else if (results.tail.isEmpty) Some(results.head)
    else throw new TooManyResults(query)
  }
}
