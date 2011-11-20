package scalad.jpa

import javax.persistence.EntityManager
import scalad.Query

/**
 * @author janmachacek
 */
object CriteriaWrapper {

  def getQuery[T](query: Query, entityManager: EntityManager, entityType: Class[T]) = {
    val cb = entityManager.getCriteriaBuilder
    val criteriaQuery = cb.createQuery(entityType)
    val root = criteriaQuery.from(entityType)

    //val predicate = cb.like(root.get[String](query.property), query.value.get.toString)
    //criteriaQuery.where(Array(predicate):_*)

    entityManager.createQuery(criteriaQuery)
  }

}
