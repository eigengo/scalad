package scalad.jdbc.operations

import java.sql.Statement
import scalad.PreparedQuery

/**
 * @author janmachacek
 */
private[operations] trait ParameterSetter {

  def parameterSetter[S <: Statement](query: PreparedQuery) = { ps: S =>
    println("Setting parameters...")
  }

}