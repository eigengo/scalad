package scalad.jpa

import javax.persistence.EntityTransaction
import scalad.transaction.PlatformTransaction

/**
 * @author janmachacek
 */
class JPAPlatformTransaction(private val transaction: EntityTransaction) extends PlatformTransaction {

  def begin() {
    transaction.begin()
  }

  def rollback() {
    transaction.rollback()
  }

  def commit() {
    transaction.commit()
  }

}



