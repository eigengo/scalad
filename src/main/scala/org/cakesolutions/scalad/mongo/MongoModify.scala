package org.cakesolutions.scalad.mongo

import com.mongodb.DBObject


trait MongoModify {

  def modify[T, K](id: K, rule: DBObject)
                  (implicit provider: CollectionProvider[T],
                   builder: KeyQueryBuilder[T, K]) {
    val col = provider.getCollection
    val query = builder.createKeyQuery(id)
    col.findAndModify(query, rule)
  }

  def findAndModify[T](query: DBObject, rule: DBObject)
                      (implicit provider: CollectionProvider[T]) {
    val col = provider.getCollection
    col.findAndModify(query, rule)
  }
}
