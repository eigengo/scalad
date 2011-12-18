package scalad.example.jdbc

import scalad.example.User
import com.mchange.v2.c3p0.ComboPooledDataSource
import scalad.Scalad
import scalad.jdbc.operations.{Iteratees, DDL}
import scalad.jdbc.{Immediate, Precompiled, JDBC}

/**
 * @author janmachacek
 */
object Main {

  import scalaz.IterV._
  import Scalad._
  import scalad.Query._

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

    val allusers = jdbc.select("select * from USER", list[User]) {rs => new User()}
    println(allusers)
    //val firstTwo = jdbc.select("select * from USER", head[User] >>= (u => head map (u2 => (u <|*|> u2)))) {rs=>new User()}
    //println(firstTwo)

    val id1 = jdbc.select("select * from USER where id = 1" | u, head[User]) {rs=>new User()}
    println(id1)

    /*
    jdbc.insert("USER (id, name, name) values (.id, .name, .name)" | u)
    jdbc.update("USER set name = .name where id = .id" | u)
    jdbc.select("* FROM USER" where "username" like u.getUsername, list[User]) {rs=>new User()}
     */

    // I want to do a simple SQL operation, such as:
    //jdbc.update("insert into USER (id, version, name) values (4, 5, 'foooo')" | u)
    //jdbc.select("update USER set name = 'foo' where id = :id" | u)

    val precompiled = new JDBC(dataSource) with Precompiled with Iteratees
    val byId = precompiled.select("SELECT * FROM USER where id=?", head[User]) {rs=>new User()}

    //byId(1L)
    //byId(2L)
  }

}