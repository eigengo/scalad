package org.cakesolutions.scalad.mongo

/** `FIND` operations – i.e. requires an "example entity" to query the database.
  */
trait MongoFind {
  this: MongoSearch =>

  /** @return the found entity or `None` if the entity was not found in the database.
    * @throws TooManyResults if more than one result.
    */
  def findUnique[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): Option[T] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchUnique(id)
  }

  /** @return the found entity or `None` if the entity was not found in the database. */
  def findFirst[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): Option[T] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchFirst(id)
  }

  /** @return all results of the query. */
  def findAll[T: CollectionProvider : MongoSerializer : IdentityQueryBuilder](entity: T): ConsumerIterator[T] = {
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)
    searchAll(id)
  }

}

// TODO: selective FIND