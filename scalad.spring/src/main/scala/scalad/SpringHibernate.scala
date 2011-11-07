package scalad

import org.springframework.orm.hibernate3.{HibernateCallback, HibernateTemplate}
import org.hibernate.Session
import org.hibernate.criterion.DetachedCriteria


/**
 * @author janmachacek
 */
class SpringHibernate(private val hiberanteTemplate: HibernateTemplate) {
  require(hiberanteTemplate != null, "The 'hibernateTemplate' must not be null.")

  import Scalad._
  import scalaz._

  def persist(entity: AnyRef) = {
    hiberanteTemplate.saveOrUpdate(entity)

    entity
  }

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    (q: QueryBuilder) =>
      val criteria = DetachedCriteria.forClass(evidence.erasure)
      val r = hiberanteTemplate.findByCriteria(criteria).iterator().asInstanceOf[java.util.Iterator[T]]

      i(r).run
  }

  def selectThat[T : ClassManifest, R](i: IterV[T, R])(q: QueryBuilder) = {
    val f = selector(i)
    f(q)
  }

  def select[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    /*
    val cb = entityManager.getCriteriaBuilder
    val criteriaQuery = cb.createQuery(evidence.erasure)
    val root = criteriaQuery.from(evidence.erasure)
    criteriaQuery.orderBy(cb.asc(root.get("username")))
    val query = entityManager.createQuery(criteriaQuery)

    val r = query.getResultList.iterator().asInstanceOf[java.util.Iterator[T]]
    i(r).run
    */
  }

}
