package scalad.jdbc.operations

import scalad.Query
import java.sql.PreparedStatement

/**
 * @author janmachacek
 */
private[operations] trait ParameterSettter {

  def parameterSetter(query: Query) = { ps: PreparedStatement =>
    println("Setting parameters...")
  }

}