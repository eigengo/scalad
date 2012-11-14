package org.cakesolutions.scalad.mongo

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
