package scalad.jdbc.operations

import java.sql.Statement
import scalad.jdbc.JDBCOperations
import scalad.PreparedQuery

/**
 * @author janmachacek
 */
trait DDL {
  this: JDBCOperations =>

  def execute(q: PreparedQuery) =
    perform[Statement, Boolean](_.createStatement(), (_=>()), (_.execute(q.query)))

}