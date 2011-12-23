package scalad.jdbc.operations

import scalad.ExecutableQuery
import java.sql.Statement

/**
 * @author janmachacek
 */
private[operations] trait ParameterSetter {

  def parameterSetter[S <: Statement](query: ExecutableQuery) = { ps: S =>
    println("Setting parameters...")
  }

}