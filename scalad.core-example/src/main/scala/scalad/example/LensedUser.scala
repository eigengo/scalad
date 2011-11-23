package scalad.example

import reflect.BeanProperty
import javax.persistence.{Version, Id, GeneratedValue, Entity}
import scalad.annotation.selectable
import scalad.SelectableSetter

/**
 * @author janmachacek
 */
@selectable
@Entity
class LensedUser {
  @BeanProperty
  @Id @GeneratedValue
  var id: Long = _
  @BeanProperty
  @Version
  var version: Int = _
  @BeanProperty
  var username: String = _
}
/*
object LensedUser {
  def id = SelectableSetter(classOf[LensedUser].getMethod("setId", classOf[Long]))
  def username = SelectableSetter(classOf[LensedUser].getMethod("setUsername", classOf[String]))
}
*/