package scalad

/**
 * Mixin that defines Persistable-like operations, specifically, allows conversions of any types into
 * {{Persistable[T]}}, defining methods like {{persist}} and {{delete}}.
 *
 * @author janmachacek
 */
trait PersistableLike {
  type DoPersist = (AnyRef) => Unit
  type DoDelete = (AnyRef) => Unit

  def underlyingPersist: DoPersist

  def underlyingDelete: DoDelete

  /**
   * Persistable introduces methods that will include methods that persist or delete the object they operate on
   */
  trait Persistable[T <: AnyRef] {

    /**
     * The entity to be operated on
     */
    def entity: T

    /**
     * Persists (e.g. insert or update) the underlying object
     *
     * @return {{T}} the entity that has just been persisted
     */
    def persist: T = {
      val e = entity
      underlyingPersist(e)
      e
    }

    /**
     * Deletes the underlying object
     */
    def delete() {
      underlyingDelete(entity)
    }

  }

  object PersistableMixin {
    implicit def innerObj[T <: AnyRef](o: Mixin[T]): T = o.entity

    def ::[T <: AnyRef](o: T) = new Mixin(o)

    final class Mixin[T <: AnyRef] private[PersistableLike](val entity: T) extends Persistable[T]
  }

  implicit def toPersistable[T <: AnyRef](entity: T): Persistable[T] = {
    entity :: PersistableMixin
  }


}