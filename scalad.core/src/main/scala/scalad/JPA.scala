package scalad

import javax.persistence.{EntityTransaction, EntityManager}
import transaction.{PlatformTransaction, PlatformTransactionManager}

/**
 * @author janmachacek
 */
class JPA(private val entityManager: EntityManager) {
  require(entityManager != null, "The 'entityManager' must not be null.")

  import Scalad._
  import scalaz._

  trait Persistable[T] {

    def entity: T

    def persistN: T = {
      val e = entity
      
      entityManager.persist(e)

      e
    }
    
    def persist: T = {
      val t = entityManager.getTransaction
      t.begin()
      val e = entity
      entityManager.persist(e)
      t.commit()

      e
    }
    
  }

  object PersistableMixin {
    implicit def innerObj[T](o: Mixin[T]): T = o.entity

    def ::[T](o: T) = new Mixin(o)

    final class Mixin[T] private[JPA](val entity: T) extends Persistable[T]
  }

  implicit def toPersistable[T](entity: T): Persistable[T] = {
    entity :: PersistableMixin
  }

  /*
  trait Selectable[T] {
    def entityType: Class[T]

    def get(id: Any): T = {
      entityManager.find(this.entityType, id).asInstanceOf[T]
    }

  }


  implicit def toSelectable[E](implicit evidence: ClassManifest[E]): Selectable[E] = {
    new Selectable[E] {
      def entityType = evidence.erasure.asInstanceOf[Class[E]]
    }
  }
  */
  
  def getPlatformTransactionManager = new JPAPlatformTransactionManager(entityManager)

  def get[T](id: Serializable)(implicit evidence: ClassManifest[T]) = {
    val entity = entityManager.find(evidence.erasure, id)
    if (entity != null)
      Some(entity.asInstanceOf[T])
    else
      None
  }

  def persist(entity: AnyRef) = {
    entityManager.persist(entity)

    entity
  }

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    (q: Query) =>
      val query = CriteriaWrapper.getQuery(q, entityManager, evidence.erasure)

      val r = query.getResultList.iterator().asInstanceOf[java.util.Iterator[T]]
      i(r).run
  }

  def sel[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    new Runner[R]({
      q =>
        val query = CriteriaWrapper.getQuery(q, entityManager, evidence.erasure)

        val r = query.getResultList.iterator().asInstanceOf[java.util.Iterator[T]]
        i(r).run
    }
    )
  }

  def selectThat[T: ClassManifest, R](i: IterV[T, R])(q: Query) = {
    val f = selector(i)
    f(q)
  }

  def select[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    val cb = entityManager.getCriteriaBuilder
    val criteriaQuery = cb.createQuery(evidence.erasure)
    criteriaQuery.from(evidence.erasure)
    val query = entityManager.createQuery(criteriaQuery)

    val r = query.getResultList.iterator().asInstanceOf[java.util.Iterator[T]]
    i(r).run
  }

  class Runner[T](op: Query => T) {

    def |(q: Query) = new RunnableQuery[T](this)

    def | = new RunnableQuery[T](this)

    def ||[T] = op(new Query("x", None))

    def run(q: Query) = op(q)

    def apply(): T = {
      run(new Query("x", None))
    }
  }

  class RunnableQuery[T](runner: Runner[T]) extends Query("x", None) {

    def | = runner.run(this)

  }

}

class JPAPlatformTransactionManager(private val entityManager: EntityManager) extends PlatformTransactionManager {

  def getTransaction = new JPAPlatformTransaction(entityManager.getTransaction)

}

class JPAPlatformTransaction(private val transaction: EntityTransaction) extends PlatformTransaction {

  def begin() {
    transaction.begin()
  }

  def rollback() {
    transaction.rollback()
  }

  def commit() {
    transaction.commit()
  }

}