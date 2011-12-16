package scalad.hibernate

import scalad._
import org.hibernate.{Session, SessionFactory}

/**
 * @author janmachacek
 */
class Hibernate3(sessionFactory: SessionFactory) extends HibernateOperations(sessionFactory)
  with PersistableLike with OrmLike {

  def inSession[R](f: (Session) => R) = {
    val session = sessionFactory.openSession()
    val result = f(session)
    session.close()

    result
  }

  def underlyingPersist[E](entity: E) {
    inSession(_.saveOrUpdate(entity))
  }

  def underlyingDelete[E](entity: E) {
    inSession(_.delete(entity))
  }

}