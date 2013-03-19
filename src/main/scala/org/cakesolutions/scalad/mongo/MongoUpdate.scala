package org.cakesolutions.scalad.mongo


/** `UPDATE` Operations. */
trait MongoUpdate {

  /** Updates the first entry that matches the identity query.
    * @return the parameter or `None` if the entity was not found in the database.
    */
  def updateFirst[T: CollectionProvider : MongoSerialiser : IdentityQueryBuilder](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerialiser[T]]
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndModify(id, serialiser serialiseDB entity) == null) None
    else Some(entity)
  }

  /** Find the old entry in the database by comparing it to the first parameter,
    * and update it with the new one. Appropriate when an identity field is changed.
    */
  def updateFirst[T: IdentityQueryBuilder: MongoSerialiser: CollectionProvider](old: T, update: T): Option[T] = {
    val col = implicitly[CollectionProvider[T]].getCollection
    val query = implicitly[IdentityQueryBuilder[T]].createIdQuery(old)
    val existing = col.findOne(query)
    if (existing == null) return None
    val updateDb = implicitly[MongoSerialiser[T]].serialiseDB(update)
    if (col.update(existing, updateDb) == null) None
    else Some(update)
  }

}
