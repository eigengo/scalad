package org.cakesolutions.scalad.mongo

/** `CREATE` operations. */
trait MongoCreate {

  /** Use unique indices in MongoDB to ensure that duplicate entries are not created
    * (`CollectionProvider` is a good place to do this).
    * @return the parameter, or `None` if not added.
    */
  def create[T: CollectionProvider : MongoSerialiser](entity: T): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerialiser[T]]

    val result = collection.insert(serialiser serialiseDB entity).getLastError
    if (result.ok()) Some(entity)
    else None
  }
}
