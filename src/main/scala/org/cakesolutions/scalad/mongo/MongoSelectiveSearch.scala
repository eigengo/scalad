package org.cakesolutions.scalad.mongo

import com.mongodb.DBObject
import collection.mutable.ArrayBuffer

/** Required by [[org.cakesolutions.scalad.mongo.MongoSelectiveSearch]].
  */
trait ResultSelector[T] {

  /** Called periodically by the selective search.
    * @return the trimmed results according to the implementation specific criteria.
    */
  def trim(results: List[T]): Iterable[T]
}

/** Allows client code to select results by trimming incremental
  * result sets.
  *
  * This avoids running into memory problems when a search may return
  * many results, but a selection criteria (e.g. "10 most recent") is
  * to be used.
  */
trait MongoSelectiveSearch {

  /** @param selector called periodically to trim the results
    * @return all results from the query and passing the selection criteria.
    *         The return type is different to
    *         [[org.cakesolutions.scalad.mongo.MongoSearch.searchAll()]]
    *         and this method blocks.
    */
  def searchAll[T: CollectionProvider : MongoSerialiser](query: DBObject, selector: List[T] => List[T]): List[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerialiser[T]]

    val cursor = collection.find(query)
    try {
      val hits = new ArrayBuffer[T]
      while (cursor.hasNext) {
        hits += serialiser deserialise cursor.next()
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