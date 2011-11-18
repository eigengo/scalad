package scalad

/**
 * @author janmachacek
 */

trait OrmLike {
  type DoPersist = (AnyRef) => Unit
  type DoDelete = (AnyRef) => Unit

  def underlyingPersist: DoPersist

  def underlyingDelete: DoDelete

  trait Persistable[T <: AnyRef] {

    def entity: T

    def persist: T = {
      val e = entity
      underlyingPersist(e)
      e
    }

    def delete() {
      underlyingDelete(entity)
    }

  }

  object PersistableMixin {
    implicit def innerObj[T <: AnyRef](o: Mixin[T]): T = o.entity

    def ::[T <: AnyRef](o: T) = new Mixin(o)

    final class Mixin[T <: AnyRef] private[OrmLike](val entity: T) extends Persistable[T]
  }

  implicit def toPersistable[T <: AnyRef](entity: T): Persistable[T] = {
    entity :: PersistableMixin
  }


}