package scalad.jdbc.operations

import scalad.jdbc.JDBCOperations
import java.sql.Statement

/**
 * @author janmachacek
 */

trait DDL {
  this: JDBCOperations =>

  def !!(sql: String) {
    execute[Statement, Unit](_.createStatement(), (_ => ()), (_.execute(sql)))
  }
  
}