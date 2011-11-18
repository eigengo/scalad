package scalad

import javax.persistence.{EntityTransaction, EntityManager}
import transaction.{PlatformTransaction, PlatformTransactionManager}

/**
 * @author janmachacek
 */
class JPA(private val entityManager: EntityManager) extends PersistableLike {
  require(entityManager != null, "The 'entityManager' must not be null.")

  import Scalad._
  import scalaz._

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

  def underlyingPersist = (entity) => transactionally(getPlatformTransactionManager) { entityManager.persist(entity) }

  def underlyingDelete = (entity) => transactionally(getPlatformTransactionManager) { entityManager.remove(entity) }
  
  implicit val platformTransactionManager = getPlatformTransactionManager
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