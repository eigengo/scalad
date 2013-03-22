package org.cakesolutions.scalad.mongo

import com.mongodb.WriteConcern

/** `CREATE` operations. */
trait MongoCreate {

  /** Use unique indices in MongoDB to ensure that duplicate entries are not created
    * (`CollectionProvider` is a good place to do this).
    * @return the parameter, or `None` if not added.
    */
  def create[T: CollectionProvider : MongoSerialiser](entity: T, concern: WriteConcern = null): Option[T] = {
    val collection = implicitly[CollectionProvider[T]].getCollection
    val serialiser = implicitly[MongoSerialiser[T]]

    val serialised = serialiser serialiseDB entity
    val result =
      if (concern == null ) collection.insert(serialised).getLastError
      else collection.insert(serialised, concern).getLastError

    if (result.ok()) Some(entity)
    else None
  }
}
