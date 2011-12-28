package scalad

import javax.persistence.{Version, Id, GeneratedValue, Entity}
import reflect.BeanProperty


/**
 * @author janmachacek
 */
@Entity
class Sample {
  @Id
  @GeneratedValue
  @BeanProperty
  var id: Long = _
  @BeanProperty
  var name: String = _

}