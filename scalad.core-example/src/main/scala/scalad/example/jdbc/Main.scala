package scalad.example.jdbc

import scalad.example.User
import com.mchange.v2.c3p0.ComboPooledDataSource
import scalad.Scalad
import scalad.jdbc.operations.{Iteratees, DDL}
import java.sql.ResultSet
import scalad.jdbc.{Immediate, Precompiled, JDBC}

/**
 * @author janmachacek
 */
object Main {

  import scalaz.IterV._
  import Scalad._

  def main(args: Array[String]) {
    val dataSource = new ComboPooledDataSource();
    dataSource.setDriverClass("org.hsqldb.jdbc.JDBCDriver")
    dataSource.setJdbcUrl("jdbc:hsqldb:mem:test");
    dataSource.setUser("sa");
    
    val u = new User
    u.setId(100)
    u.setUsername("asfasfsd")

    val jdbc = new JDBC(dataSource) with DDL with Iteratees with Immediate

    // I want to perform arbitrary DDL
    jdbc.execute("create table USER (id INT PRIMARY KEY, version INT, name VARCHAR(200))")
    jdbc.execute("INSERT INTO USER (id, version, name) values (1, 1, 'foo')")
    jdbc.execute("INSERT INTO USER (id, version, name) values (2, 1, 'bar')")
    jdbc.execute("INSERT INTO USER (id, version, name) values (3, 1, 'baz')")

    val mapper = (rs: ResultSet) => {
      val u = new User
      u.setId(rs.getLong("id"))
      u.setUsername(rs.getString("name"))

      u
    }
    
    val allusers = jdbc.select("select * from USER", list[User])(mapper)
    println(allusers)
//    val firstTwo = jdbc.select("select * from USER", head[User] >>= (u => head map (u2 => (u <|*|> u2))))(mapper)
//    println(firstTwo)

    //jdbc.select("* from USER" where ("id" ＝ 5L), head[User])(mapper)

    val id1 = jdbc.select("select * from USER where id = ?" | 1L, head[User])(mapper)
    println(id1.get)

    val mappedJdbc = new JDBC(dataSource) with Iteratees with Immediate

    val allMapped = mappedJdbc.select("select * from USER", list[User])(mapper)
    println(allMapped)

    /*
    jdbc.insert("USER (id, name, name) values (.id, .name, .name)" | u)
    jdbc.update("USER set name = .name where id = .id" | u)
    jdbc.select("* FROM USER" where "username" like u.getUsername, list[User]) {rs=>new User()}
     */

    // I want to do a simple SQL operation, such as:
    //jdbc.update("insert into USER (id, version, name) values (4, 5, 'foooo')" | u)
    //jdbc.select("update USER set name = 'foo' where id = :id" | u)

    val precompiled = new JDBC(dataSource) with Precompiled with Iteratees
    val byId =
      precompiled.select("SELECT * FROM USER where id=? and name=?" | (1L, "foo"), head[User])(mapper)

    byId("foo")
    byId("bar")
  }

}