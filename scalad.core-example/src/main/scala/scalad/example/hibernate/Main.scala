package scalad.example.hibernate

import scalad.example.{UserAddress, User}
import scalad.hibernate.Hibernate3
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

/**
 * @author janmachacek
 */
object Main {

  import scalad._
  import Scalad._

  def main(args: Array[String]) {
    val sessionFactory = new Configuration()
            .configure()
            .addAnnotatedClass(classOf[User])
            .addAnnotatedClass(classOf[UserAddress])
            .buildSessionFactory();
    
    new Worker(sessionFactory).work()
  }

  class Worker(sessionFactory: SessionFactory) extends Hibernate3(sessionFactory) {

    def work() {
      for (i <- 0 to 20) {
        val u = new User()
        u.setUsername("a" + i)
        for (j <- 0 to 5) {
          val ua = new UserAddress
          ua.setLine1("L1")
          ua.setLine2("L2")
          u.addAddress(ua)
        }

        u.persist
      }

      val users = selector(list[User])
      val selectedUsers = users((id ＝ 5L) && (id !＝ 5L))
      println(selectedUsers)

      val username = "a1%"
      val someUsers = users((id ＝ 3L) || ("username" like username when username != ""))
      println(someUsers)
    }

  }

}