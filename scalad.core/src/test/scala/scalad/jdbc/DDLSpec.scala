package scalad.jdbc

import operations.{DDL, Iteratees}
import javax.sql.DataSource
import scalad.Scalad

/**
 * @author janmachacek
 */
class DDLSpec extends JDBCSpecification {
  import Scalad._

  override type JDBCType = JDBC with DDL with Iteratees with Immediate

  def jdbc(dataSource: DataSource) = new JDBC(dataSource) with DDL with Iteratees with Immediate

  "execute DDL statements" in {
    instance.execute("CREATE TABLE USER (id INT PRIMARY KEY, version INT, name VARCHAR(200))")
    instance.execute("INSERT INTO USER (id, version, name) values (1, 1, 'foo')")
    instance.execute("DROP TABLE USER")

    success
  }

}