package scalad.jdbc.operations

import scalad.jdbc.JDBCOperations
import java.sql.Statement
import scalad.Query

/**
 * @author janmachacek
 */

trait DDL {
  this: JDBCOperations =>

  def execute(sql: String) {
    perform[Statement, Unit](_.createStatement(), (_ => ()), (_.execute(sql)))
  }
  
  def select(query: Query) = None
  
}