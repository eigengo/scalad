package scalad.jdbc.operations

import scalad.{SettableParameter, PreparedQuery}
import java.sql.{Types, PreparedStatement}

/**
 * @author janmachacek
 */
private[operations] trait ParameterSetter {
  
  def parameterSetter(query: PreparedQuery) = { ps: PreparedStatement =>
    query.foreachParam { p =>
      if (p.value == null) {
        // we don't like nulls, because there's not a lot we
        // can do about their type
        ps.setNull(p.index + 1, Types.NULL)
      } else {
        p.value match {
          case i: Int => ps.setInt(p.index + 1, i)
          case l: Long => ps.setLong(p.index + 1, l)
          case s: String => ps.setString(p.index + 1, s)
          case s: Short => ps.setShort(p.index + 1, s)
          case b: Boolean => ps.setBoolean(p.index + 1, b)
        }
      }
    }
  }

}