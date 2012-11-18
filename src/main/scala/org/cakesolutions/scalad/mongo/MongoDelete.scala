package org.cakesolutions.scalad.mongo

/** `DELETE` operations. */
trait MongoDelete {

  /** @return `None` if the delete failed, otherwise the parameter. */
  def deleteFirst[T: CollectionProvider : IdentityQueryBuilder](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val id = implicitly[IdentityQueryBuilder[T]].createIdQuery(entity)

    if (collection.findAndRemove(id) != null) Some(entity)
    else None
  }

}
