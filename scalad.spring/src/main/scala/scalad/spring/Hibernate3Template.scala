package scalad.spring

import org.springframework.orm.hibernate3.HibernateTemplate
import org.hibernate.criterion.DetachedCriteria
import scalad.QueryBuilder


/**
 * @author janmachacek
 */
class Hibernate3Template(private val hiberanteTemplate: HibernateTemplate) {
  require(hiberanteTemplate != null, "The 'hibernateTemplate' must not be null.")

  import scalad.Scalad._
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
    val criteria = DetachedCriteria.forClass(evidence.erasure)
    val r = hiberanteTemplate.findByCriteria(criteria).iterator().asInstanceOf[java.util.Iterator[T]]

    i(r).run
  }

}
