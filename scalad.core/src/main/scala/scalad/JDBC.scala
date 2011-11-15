package scalad

import javax.sql.DataSource
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
  
  def begin() { ConnectionHolder.get(dataSource).setAutoCommit(false) }

  def rollback() { ConnectionHolder.get(dataSource).rollback() }

  def commit() { ConnectionHolder.get(dataSource).commit() }
  
}

private[scalad] object ConnectionHolder {
  private val holder: ThreadLocal[Connection] = new InheritableThreadLocal[Connection]
  
  def set(connection: Connection) { holder.set(connection) }

  def clear() { holder.set(null) }
  
  def get(dataSource: DataSource) = {
    val connection = holder.get()
    if (connection == null) set(dataSource.getConnection)
    
    holder.get()
  }
  
}