package org.eigengo.scalad.mongo

/** `UPDATE OR CREATE` Operations. */
trait MongoCreateOrUpdate {
  this: MongoUpdate with MongoCreate =>

  /** Updates the first entry that matches the identity query or creates a new entry if
    * none was found. Involves two hits to the DB.
    * @return the parameter or `None` if the create failed.
    */
  def createOrUpdateFirst[T: CollectionProvider : MongoSerialiser : IdentityQueryBuilder](entity: T): Option[T] = {
    val updated = updateFirst(entity)
    updated match {
      case Some(_) =>
        updated
      case None =>
        create(entity)
    }
  }
}
