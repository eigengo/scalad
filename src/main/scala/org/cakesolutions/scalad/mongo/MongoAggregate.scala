package org.cakesolutions.scalad.mongo

import com.mongodb.{AggregationOutput, BasicDBObject, DBObject}
import collection.convert.{WrapAsScala, WrapAsJava}

trait MongoAggregate {

  def aggregate[T: CollectionProvider](pipeline: DBObject*): List[DBObject] = {
    require(!pipeline.isEmpty)
    val collection = implicitly[CollectionProvider[T]].getCollection
    val command = new BasicDBObject("aggregate", collection.getName)
    command.put("pipeline", WrapAsJava.seqAsJavaList(pipeline))
    val res = collection.getDB.command(command)
    res.throwOnError()
    val results = new AggregationOutput(command, res).results
    WrapAsScala.iterableAsScalaIterable(results).toList
  }

  private val countCommand = Implicits.JSON2DBObject("""{"$group": {"_id": null, "count": {"$sum": 1 }}}""")

  def aggregateCount[T: CollectionProvider](pipeline: DBObject*): Long = {
    val parts = pipeline.toList ::: countCommand :: Nil
    aggregate(parts: _*).toList match {
      case Nil => 0L
      case res :: Nil => res.asInstanceOf[BasicDBObject].getLong("count")
      case multi => throw new IllegalStateException(multi.toString)
    }
  }
}
