package scalad.example

import reflect.BeanProperty
import javax.persistence.{Version, Id, GeneratedValue, Entity}
import scalad.annotation.selectable

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
