package scalad.example.spring

import org.springframework.context.support.ClassPathXmlApplicationContext

/**
 * @author janmachacek
 */
object Main {

  def main(args: Array[String]) {
    val applicationContext = new ClassPathXmlApplicationContext("classpath*:/META-INF/spring/module-context.xml")
    applicationContext.getBean(classOf[Worker]).work()
  }

}