package scalad.jdbc

import operations.{DDL, Iteratees}
import org.specs2.mutable.Specification
import javax.sql.DataSource
import scalad.{Sample, Scalad}

/**
 * @author janmachacek
 */
class IterateesSpec extends Specification with JDBCSpecSupport {
  import Scalad._

  type JDBCType = JDBC with Iteratees with DDL with Immediate

  def jdbc(dataSource: DataSource) = new JDBC(dataSource) with Iteratees with DDL with Immediate

  "select all objects" in {
    setup()
    "must select all users with list" in {
      instance.select("select * from SAMPLE", list[Sample]).automap.size must be_== (3)
    }
  }

  private def setup() {
    instance.execute("create table SAMPLE (id INT PRIMARY KEY, name VARCHAR(200))")
    instance.execute("INSERT INTO SAMPLE (id, name) values (1, 'foo')")
    instance.execute("INSERT INTO SAMPLE (id, name) values (2, 'bar')")
    instance.execute("INSERT INTO SAMPLE (id, name) values (3, 'baz')")
  }
}