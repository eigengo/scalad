package scalad.jdbc

import javax.sql.DataSource
import java.sql.Connection
import java.lang.InheritableThreadLocal
import scalad.transaction.{PlatformTransaction, PlatformTransactionManager}
import scalaz.IterV
import scalad.PersistableLike

/**
 * @author janmachacek
 */

class JDBC(private val dataSource: DataSource) {

  def selector[T, R](i: IterV[T, R])(implicit evidence: ClassManifest[T]) = {
    
  }

}

class MappingJDBC(dataSource: DataSource) extends JDBC(dataSource) with PersistableLike {
  this: InsertOrUpdateVoter with Inserter with Updater with Deleter =>

  def underlyingPersist[E](entity: E) {
    if (isInsert(entity)) insert(entity, ConnectionHolder.get(dataSource)) else update(entity, ConnectionHolder.get(dataSource))
  }

  def underlyingDelete[E](entity: E) {
    delete(entity, ConnectionHolder.get(dataSource))
  }

  def persist[E](entity: E) = underlyingDelete(entity)

  def delete[E](entity: E) = underlyingDelete(entity)

}

trait InsertOrUpdateVoter {

  def isInsert[E](entity: E): Boolean

}

trait Inserter {

  def insert[E](entity: E, connection: Connection)
  
}

trait Updater {

  def update[E](entity: E, connection: Connection)
  
}

trait Deleter {

  def delete[E](entity: E, connection: Connection)

}

class JDBCPlatformTransactionManager(private val dataSource: DataSource) extends PlatformTransactionManager {

  def getTransaction = new JDBCPlatformTransaction(dataSource)

}

class JDBCPlatformTransaction(private val dataSource: DataSource) extends PlatformTransaction {

  def begin() { ConnectionHolder.get(dataSource).setAutoCommit(false) }

  def rollback() { ConnectionHolder.get(dataSource).rollback() }

  def commit() { ConnectionHolder.get(dataSource).commit() }

}

private[jdbc] object ConnectionHolder {
  private val holder: ThreadLocal[Connection] = new InheritableThreadLocal[Connection]

  def set(connection: Connection) { holder.set(connection) }

  def clear() { holder.set(null) }

  def get(dataSource: DataSource) = {
    val connection = holder.get()
    if (connection == null) set(dataSource.getConnection)

    holder.get()
  }

}