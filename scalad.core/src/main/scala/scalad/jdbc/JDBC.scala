package scalad.jdbc

import javax.sql.DataSource
import java.sql.{Statement, Connection}

trait JDBCOperations {
  type StatementCreator[S <: Statement] = (Connection) => S
  type StatementSetter[S <: Statement] = (S) => Unit
  type StatementExecutor[S <: Statement, R] = (S) => R

  def execute[S <: Statement, R](statementCreator: StatementCreator[S],
           statementSetter: StatementSetter[S],
           statementExecutor: StatementExecutor[S, R]): R

}

class JDBC(private val dataSource: DataSource) extends JDBCOperations {
  type ConnectionOperation[R] = (Connection) => R

  def withConnection[R](operation: ConnectionOperation[R]): R = {
    val connection = dataSource.getConnection
    try {
      operation(connection)
    } finally {
      connection.commit()
      connection.close()
    }
  }    
  
  def apply[R](operation: ConnectionOperation[R]): R = withConnection(operation)

  /**
   * Core operation
   */
  def execute[S <: Statement, R](statementCreator: StatementCreator[S],
           statementSetter: StatementSetter[S],
           statementExecutor: StatementExecutor[S, R]): R =
    withConnection {c =>
      val preparedStatement = statementCreator(c)
      statementSetter(preparedStatement)
      statementExecutor(preparedStatement)
    }


  def apply(entity: Any) = this

  //new Executor(entity)

  class Executor(entity: Any) {
    
    def !(sql: String) {
      
    }

  }

}

/*
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
*/