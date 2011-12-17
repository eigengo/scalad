package scalad.example.jdbc

import javax.sql.DataSource
import scalad.example.User
import com.mchange.v2.c3p0.ComboPooledDataSource
import scalad.jdbc.{HeuristicSQL, JDBC, AnnotationSQL, MappingJDBC}
import scalad.jdbc.operations.{Iteratees, DDL}
import scalad.Scalad
import java.sql.{ResultSet, Connection}

/**
 * @author janmachacek
 */
object Main {

  import scalaz.IterV._
  import Scalad._
  import scalaz._
  import Scalaz._

  def main(args: Array[String]) {
    val dataSource = new ComboPooledDataSource();
    dataSource.setDriverClass("org.hsqldb.jdbc.JDBCDriver")
    dataSource.setJdbcUrl("jdbc:hsqldb:mem:test");
    dataSource.setUser("sa");
    
    val u = new User
    u.setId(100)
    u.setUsername("asfasfsd")

    val jdbc = new JDBC(dataSource) with DDL with Iteratees

    // I want to perform arbitrary DDL
    jdbc("create table USER (id INT PRIMARY KEY, version INT, name VARCHAR(200))")
    jdbc("INSERT INTO USER (id, version, name) values (1, 1, 'foo')")
    jdbc("INSERT INTO USER (id, version, name) values (2, 1, 'bar')")
    jdbc("INSERT INTO USER (id, version, name) values (3, 1, 'baz')")

    val allusers = jdbc("select * from USER", list[User]) {rs => new User()}
    println(allusers)
    //val firstTwo = jdbc("select * from USER", head[User] >>= (u => head map (u2 => (u <|*|> u2)))) {rs=>new User()}
    //println(firstTwo)


    /*
    jdbc.insert("USER (id, name, name) values (.id, .name, .name)" | u)
    jdbc.update("USER set name = .name where id = .id" | u)
    jdbc.select("* FROM USER" where "username" like u.getUsername, list[User]) {rs=>new User()}
     */

    // I want to do a simple SQL operation, such as:
    jdbc(u)("insert into USER (id, version, name) values (.id, .version, .name)")
    jdbc(u)("update USER set name = .name where id = .id")


    val mappingJdbc = new MappingJDBC(dataSource) with AnnotationSQL
    mappingJdbc(u).delete

    new Worker(dataSource).work()
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