package scalad.example.jdbc

import javax.sql.DataSource
import scalad.example.User
import com.mchange.v2.c3p0.ComboPooledDataSource
import scalad.jdbc.{HeuristicSQL, JDBC, AnnotationSQL, MappingJDBC}

/**
 * @author janmachacek
 */
object Main {

  def main(args: Array[String]) {
    val cpds = new ComboPooledDataSource();
    cpds.setDriverClass("org.hsqldb.jdbc.JDBCDriver")
    cpds.setJdbcUrl("jdbc:hsqldb:mem:test");
    cpds.setUser("sa");

    val jdbc = new MappingJDBC(cpds) with AnnotationSQL
    val u = new User
    u.setId(100)
    u.setUsername("asfasfsd")

    jdbc.delete(u)

    new Worker(cpds).work()
  }

  class Worker(ds: DataSource) extends MappingJDBC(ds) with AnnotationSQL {

    def work() {
      val u = new User
      u.setId(100)
      u.setUsername("asfasfsd")
      
      u.delete()
    }

  }

}