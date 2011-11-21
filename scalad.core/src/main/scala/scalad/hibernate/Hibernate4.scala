package scalad.hibernate

import org.hibernate.SessionFactory
import scalaz.IterV
import java.util.ArrayList
import scalad._

/**
 * @author janmachacek
 */

class Hibernate4(private val sessionFactory: SessionFactory) extends PersistableLike with OrmLike {

  import Scalad._

  def underlyingPersist = { e => sessionFactory.getCurrentSession.saveOrUpdate(e) }

  def underlyingDelete = { e => sessionFactory.getCurrentSession.delete(e) }

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    (q: OrmQuery) =>
      val simplifiedQuery = q.simplify

      val results =
        if (simplifiedQuery.restriction == Contradiction())
          new java.util.ArrayList[T]().iterator()
        else
          // hard work
          throw new Exception("Implement me")


      i(results).run
  }
}