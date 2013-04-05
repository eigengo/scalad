package org.eigengo.scalad.mongo

import com.mongodb.DBObject

trait MongoCount {

  def count[T](query: DBObject)
              (implicit provider: CollectionProvider[T]): Long =
    provider.getCollection.count(query)

}
