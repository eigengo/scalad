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
    for (i <- 0 until 100)
      instance.execute("INSERT INTO SAMPLE (id, name) values (" + i + ", 'Name" + i + "')")()

    val start = System.currentTimeMillis()
    val s1 = instance.select("SELECT * FROM SAMPLE", list[Sample]){rs=>new Sample}
    val s2 = instance.select("SELECT * FROM SAMPLE", list[Sample]){rs=>new Sample}

    "s1 and s2 should return immediately" in {
      System.currentTimeMillis() - start must be_< (1000L)
    }
    "calling ! on s1 and s2 should give the results" in {
      s1().size must_== (100)
      s2().size must_== (100)
    }
  }

}