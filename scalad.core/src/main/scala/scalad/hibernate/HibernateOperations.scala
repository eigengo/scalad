package scalad.hibernate

import scalaz.IterV
import org.hibernate.criterion.{Order, Restrictions, Criterion}
import scalad._
import org.hibernate.{SessionFactory, FetchMode}

/**
 * @author janmachacek
 */

abstract class HibernateOperations(private val sessionFactory: SessionFactory) {
  import Scalad._

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    new Selector[T, R, OrmQuery]({
      (q, t) =>
        val simplifiedQuery = q.simplify

        val session = sessionFactory.openStatelessSession()
        val criteria = session.createCriteria(evidence.erasure)

        def getCriterion(r: Restriction): Option[Criterion] = r match {
          case Binary(t(NamedProperty(p)), '==, v) => Some(Restrictions.eq(p, v))
          case Binary(t(Identity()), '==, v) => Some(Restrictions.idEq(v))
          case Binary(t(NamedProperty(p)), '!=, v) => Some(Restrictions.ne(p, v))

          case Binary(t(NamedProperty(p)), '>, v) => Some(Restrictions.gt(p, v))
          case Binary(t(NamedProperty(p)), '>=, v) => Some(Restrictions.ge(p, v))
          case Binary(t(NamedProperty(p)), '<, v) => Some(Restrictions.lt(p, v))
          case Binary(t(NamedProperty(p)), '<=, v) => Some(Restrictions.le(p, v))

          case IsNull(t(NamedProperty(p))) => Some(Restrictions.isNull(p))
          case IsNotNull(t(NamedProperty(p))) => Some(Restrictions.isNotNull(p))

          case In(t(NamedProperty(p)), v) => Some(Restrictions.in(p, v.map(_.asInstanceOf[AnyRef]).toArray))
          case Like(t(NamedProperty(p)), v) => Some(Restrictions.like(p, v))

          case Disjunction(lhs, rhs) => Some(Restrictions.or(getCriterion(lhs).get, getCriterion(rhs).get))
          case Conjunction(lhs, rhs) => Some(Restrictions.and(getCriterion(lhs).get, getCriterion(rhs).get))
          case Tautology() => None
          case Contradiction() => None
          case Skip() => None
        }

        // joins
        simplifiedQuery.joins.foreach {
          join =>
            if (join.eager) criteria.setFetchMode(join.path.expression, FetchMode.JOIN)
        }

        simplifiedQuery.orderByClauses.foreach(_ match {
          case Asc(NamedProperty(p)) => criteria.addOrder(Order.asc(p))
          case Desc(NamedProperty(p)) => criteria.addOrder(Order.desc(p))
          // order by ids!
        })

        // group bys

        val results = simplifiedQuery.restriction match {
          case Contradiction() => new java.util.ArrayList[T]().iterator()
          case r => getCriterion(r) match {
            case Some(r2) =>
              criteria.add(r2)
              criteria.list().iterator().asInstanceOf[java.util.Iterator[T]]
            case None =>
              new java.util.ArrayList[T]().iterator()
          }
        }

        session.close()

        i(results).run
    })

  }

}