package scalad.jdbc.operations

import java.sql.Statement
import scalad.Query
import scalad.jdbc.{ExecutionPolicy, JDBCOperations}

/**
 * @author janmachacek
 */

trait DDL {
  this: JDBCOperations with ExecutionPolicy =>

  def execute(sql: String) = exec {
    perform[Statement, Unit](_.createStatement(), (_ => ()), (_.execute(sql)))
  }
  
  def select(query: Query) = None
  
}