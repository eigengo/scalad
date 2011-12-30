package scalad.jdbc

import operations.{DDL, Iteratees}
import javax.sql.DataSource
import scalad.{Sample, Scalad}
import java.sql.ResultSet

/**
 * @author janmachacek
 */
class IterateesSpec extends JDBCSpecification {
  import Scalad._

  type JDBCType = JDBC with Iteratees with DDL with Immediate

  def jdbc(dataSource: DataSource) = new JDBC(dataSource) with Iteratees with DDL with Immediate

  "select all objects" in {
    setup()
    val mapper = (rs: ResultSet) => new Sample(rs.getLong("id"), rs.getString("name"))
    "must select all users with list" in {
      instance.select("select * from SAMPLE order by ID", list[Sample])(mapper) must_==
        List(new Sample(1L, "foo"), new Sample(2L, "bar"), new Sample(3L, "baz"))
    }
  }

  private def setup() {
    instance.execute("create table SAMPLE (id INT PRIMARY KEY, name VARCHAR(200))")
    instance.execute("INSERT INTO SAMPLE (id, name) values (1, 'foo')")
    instance.execute("INSERT INTO SAMPLE (id, name) values (2, 'bar')")
    instance.execute("INSERT INTO SAMPLE (id, name) values (3, 'baz')")
  }
}