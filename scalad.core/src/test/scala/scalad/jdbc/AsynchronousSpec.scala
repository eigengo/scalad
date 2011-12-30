package scalad.jdbc

import operations.{Iteratees, DDL}
import javax.sql.DataSource
import scalad.{Sample, Scalad}

/**
 * @author janmachacek
 */
class AsynchronousSpec extends JDBCSpecification {
  import Scalad._

  override type JDBCType = JDBC with DDL with Iteratees with Asynchronous

  def jdbc(dataSource: DataSource) = new JDBC(dataSource) with DDL with Iteratees with Asynchronous

  "execute DDL statements" in {
    instance.execute("create table SAMPLE (id INT PRIMARY KEY, name VARCHAR(200))")()
    for (i <- 0 until 10000)
      instance.execute("INSERT INTO SAMPLE (id, name) values (" + i + ", 'Name" + i + "')")()

    val start = System.currentTimeMillis()
    val s1 = instance.select("SELECT * FROM SAMPLE", list[Sample]){rs=>new Sample}
    val s2 = instance.select("SELECT * FROM SAMPLE", list[Sample]){rs=>new Sample}
    val end = System.currentTimeMillis() - start

    "s1 and s2 should return immediately" in {
      end must be_< (800L)
    }
    "calling ! on s1 and s2 should give the results" in {
      s1().size must_== (10000)
      s2().size must_== (10000)
    }
  }

}