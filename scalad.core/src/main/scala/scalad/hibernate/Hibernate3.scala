package scalad.hibernate

import scalaz.IterV
import scalad._
import org.hibernate.criterion.{Order, Criterion, Restrictions, DetachedCriteria}
import org.hibernate.{Session, StatelessSession, SessionFactory}

/**
 * @author janmachacek
 */

class Hibernate3(private val sessionFactory: SessionFactory) extends PersistableLike with OrmLike {

  import Scalad._

  def inSession[R](f: (Session) => R) = {
    val session = sessionFactory.openSession()
    val result = f(session)
    session.close()

    result
  }
  
  def underlyingPersist = { e => inSession(_.saveOrUpdate(e)) }

  def underlyingDelete = { e => inSession(_.delete(e)) }

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    (q: OrmQuery) =>
      val simplifiedQuery = q.simplify
      
      val session = sessionFactory.openStatelessSession()
      val criteria = session.createCriteria(evidence.erasure)
      
      def getCriterion(r: Restriction): Option[Criterion] = r match {
        case Binary(NamedProperty(p), '==, v) => Some(Restrictions.eq(p, v))
        case Binary(Identity(), '==, v) => Some(Restrictions.idEq(v))
        case Like(NamedProperty(p), v) => Some(Restrictions.like(p, v))
        // and others
        case Disjunction(lhs, rhs) => Some(Restrictions.or(getCriterion(lhs).get, getCriterion(rhs).get))
        case Conjunction(lhs, rhs) => Some(Restrictions.and(getCriterion(lhs).get, getCriterion(rhs).get))
        case Tautology() => None
        case Contradiction() => None
        case Skip() => None
      }
      
      getCriterion(simplifiedQuery.restriction) match {
        case None =>
        case Some(c) => criteria.add(c)
      }

      simplifiedQuery.orderByClauses.foreach(_ match {
        case Asc(NamedProperty(p)) => criteria.addOrder(Order.asc(p))
        case Desc(NamedProperty(p)) => criteria.addOrder(Order.desc(p))
        // order by ids!
      })

      // group bys

      val results =
        if (simplifiedQuery.restriction == Contradiction())
          new java.util.ArrayList[T]().iterator()
        else
          criteria.list().iterator().asInstanceOf[java.util.Iterator[T]]

      session.close()
      
      i(results).run
  }
}