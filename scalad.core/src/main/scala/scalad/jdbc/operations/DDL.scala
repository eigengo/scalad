package scalad.jdbc.operations

import java.sql.Statement
import scalad.jdbc.{ExecutionPolicy, JDBCOperations}
import scalad.{PreparedQuery, Query}

/**
 * @author janmachacek
 */
trait DDL {
  this: JDBCOperations with ExecutionPolicy =>

  def execute(q: PreparedQuery) = exec {
    perform[Statement, Unit](_.createStatement(), (_=>()), (_.execute(q.query)))
  }
  
  def select(query: Query) = None
  
}