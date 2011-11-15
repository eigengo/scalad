package scalad

import javax.sql.DataSource
import javax.persistence.{EntityTransaction, EntityManager}
import transaction.{PlatformTransaction, PlatformTransactionManager}
import java.sql.Connection
import java.lang.InheritableThreadLocal

/**
 * @author janmachacek
 */

class JDBC(private val dataSource: DataSource) {

}

class JDBCPlatformTransactionManager(private val dataSource: DataSource) extends PlatformTransactionManager {

  def getTransaction = new JDBCPlatformTransaction(dataSource)

}

class JDBCPlatformTransaction(private val dataSource: DataSource) extends PlatformTransaction {
  
  def begin() {  }

  def rollback() {  }

  def commit() {  }
  
}

private[scalad] object ConnectionHolder {
  private val holder: ThreadLocal[Connection] = new InheritableThreadLocal[Connection]
  
  def set(connection: Connection) { holder.set(connection) }
  
  def get() = holder.get()
  
}