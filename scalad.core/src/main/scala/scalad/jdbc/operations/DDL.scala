package scalad.jdbc.operations

import java.sql.Statement
import scalad.jdbc.{ExecutionPolicy, JDBCOperations}
import scalad.{PreparedQuery, Query}

/**
 * @author janmachacek
 */
trait DDL extends ParameterSetter {
  this: JDBCOperations with ExecutionPolicy =>

  def execute(q: PreparedQuery) = exec {
    perform[Statement, Unit](_.createStatement(), parameterSetter(q), (_.execute(q.query)))
  }
  
  def select(query: Query) = None
  
}