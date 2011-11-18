package scalad.example.spring

import scalad.example.User
import scalad.Scalad._
import scalaz.IterV._
import org.springframework.orm.hibernate3.HibernateTemplate
import scalad.spring.Hibernate3Template
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author janmachacek
 */
@Component
class Worker @Autowired() (val hibernateTemplate: HibernateTemplate) extends Hibernate3Template(hibernateTemplate) {

  import scalaz._
  import Scalaz._
  
  def work() {
    for (i <- 0 to 20) {
      val u = new User()
      u.setUsername("a" + i)
      u.persist
    }

    val users = select(list[User])
    println(users)

    //val m1 = head[User] >>= (u => head map (u2 => (u <|*|> u2)))
    //val firstTwo = selector(m1)
    // println(firstTwo("username" like "a2%"))

    //select(one[User])
    //selectThat(one[User])("username" like "B")

    //val usersWhose = selectThat(head[User])("username" like "a1%")
    //println(usersWhose)
  }

}