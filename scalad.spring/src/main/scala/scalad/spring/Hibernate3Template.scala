package scalad.spring

import org.springframework.orm.hibernate3.HibernateTemplate
import org.hibernate.criterion.DetachedCriteria
import scalad.{OrmLike, Query}


/**
 * @author janmachacek
 */
class Hibernate3Template(private val hiberanteTemplate: HibernateTemplate) extends OrmLike {
  require(hiberanteTemplate != null, "The 'hibernateTemplate' must not be null.")

  import scalad.Scalad._
  import scalaz._

  def persist(entity: AnyRef) = {
    hiberanteTemplate.saveOrUpdate(entity)

    entity
  }

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    (q: Query) =>
      val criteria = DetachedCriteria.forClass(evidence.erasure)
      val r = hiberanteTemplate.findByCriteria(criteria).iterator().asInstanceOf[java.util.Iterator[T]]

      i(r).run
  }

  def selectThat[T : ClassManifest, R](i: IterV[T, R])(q: Query) = {
    val f = selector(i)
    f(q)
  }

  def select[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    val criteria = DetachedCriteria.forClass(evidence.erasure)
    val r = hiberanteTemplate.findByCriteria(criteria).iterator().asInstanceOf[java.util.Iterator[T]]

    i(r).run
  }

  def underlyingPersist = (entity) => hiberanteTemplate.saveOrUpdate(entity)

  def underlyingDelete = (entity) => hiberanteTemplate.delete(entity)
  
}
