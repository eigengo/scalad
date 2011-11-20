package scalad.jpa

import javax.persistence.{EntityTransaction, EntityManager}
import scalad.transaction.{PlatformTransactionManager, PlatformTransaction}

/**
 * @author janmachacek
 */
class JPAPlatformTransactionManager(private val entityManager: EntityManager) extends PlatformTransactionManager {

  def getTransaction = new JPAPlatformTransaction(entityManager.getTransaction)

}



