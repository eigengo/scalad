package scalad.example.jpa

import javax.persistence.{EntityManager, Persistence}
import scalad.transaction.Transactions
import scalad.jpa.JPA
import scalad.example.{UserAddress, User}

/**
 * @author janmachacek
 */
object Main {

  import scalad._
  import Scalad._

  def main(args: Array[String]) {
    val entityManagerFactory = Persistence.createEntityManagerFactory("org.cakesolutions.scaladata.core.example")
    val entityManager = entityManagerFactory.createEntityManager()
    new Worker(entityManager).work()
  }

  class Worker(entityManager: EntityManager) extends JPA(entityManager) with Transactions {

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
      println(users("username" like "a%" innerJoinFetch "addresses"))
    }

  }

}