package scalad.jpa

import javax.persistence.EntityManager

/**
 * @author janmachacek
 */
object CriteriaWrapper {

  def getQuery[T](query: JPAQuery, entityManager: EntityManager, entityType: Class[T]) = {
    val cb = entityManager.getCriteriaBuilder
    val criteriaQuery = cb.createQuery(entityType)
    val root = criteriaQuery.from(entityType)

    query.joins.foreach {join =>
      root.fetch(join.path)
    }
    
    //val predicate = cb.like(root.get[String](query.property), query.value.get.toString)
    //criteriaQuery.where(Array(predicate):_*)

    entityManager.createQuery(criteriaQuery)
  }

}
