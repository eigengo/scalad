package scalad.jpa

import javax.persistence.EntityManager
import scalad._

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
    (q: JPAQuery) =>
      val simplifiedQuery = q.simplify
      val query = CriteriaWrapper.getQuery(simplifiedQuery, entityManager, evidence.erasure)

      val r = query.getResultList.iterator().asInstanceOf[java.util.Iterator[T]]
      i(r).run
  }

  def selectThat[T: ClassManifest, R](i: IterV[T, R])(q: JPAQuery) = {
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
  
  def flush() { entityManager.flush() }
  
  def flushAndClear() { entityManager.flush(); entityManager.clear(); }

  def underlyingPersist = (entity) => transactionally(getPlatformTransactionManager) { entityManager.persist(entity) }

  def underlyingDelete = (entity) => transactionally(getPlatformTransactionManager) { entityManager.remove(entity) }
  
  implicit val platformTransactionManager = getPlatformTransactionManager

  implicit def toJPAQuery(q: Query) = new JPAQuery(q.restriction, q.orderByClauses, q.groupByClauses, None, Nil)

  implicit def toJPAQuery(r: Restriction) = new JPAQuery(r, Nil, Nil, None, Nil)
  
  implicit def toPath(expression: String) = new PartialPath(expression)

}