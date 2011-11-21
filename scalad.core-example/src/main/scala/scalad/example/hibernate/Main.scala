package scalad.example.hibernate

import javax.persistence.{EntityManager, Persistence}
import scalad.transaction.Transactions
import scalad.jpa.JPA
import scalad.example.{UserAddress, User}
import scalad.hibernate.Hibernate4
import org.hibernate.{Hibernate, SessionFactory}
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
            .buildSessionFactory();
    
    new Worker(sessionFactory).work()
  }

  class Worker(sessionFactory: SessionFactory) extends Hibernate4(sessionFactory) {

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
      val selectedUsers = users((id is 5) && (id isNot 5))
      println(selectedUsers)

      val username = "x"  // input from the users
      val someUsers = users((id is 5) || ("username" like username when username != ""))
      println(selectedUsers)
    }

  }

}