package scalad.jdbc.operations

import scalad.PreparedQuery
import java.sql.PreparedStatement

/**
 * @author janmachacek
 */
private[operations] trait ParameterSetter {

  def parameterSetter(query: PreparedQuery) = { ps: PreparedStatement =>
    query.foreachParams { p =>
      ps.setObject(p.index + 1, p.value)
    }
  }

}