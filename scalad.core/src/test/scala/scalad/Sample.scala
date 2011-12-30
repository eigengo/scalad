package scalad

import javax.persistence.{Version, Id, GeneratedValue, Entity}
import reflect.BeanProperty


/**
 * @author janmachacek
 */
@Entity
class Sample(val _id: Long, val _name: String) {
  
  def this() = this(0L, "")
  
  @Id
  @GeneratedValue
  @BeanProperty
  var id: Long = _id
  @BeanProperty
  var name: String = _name

  override def equals(p1: Any) = {
    if (!p1.isInstanceOf[Sample]) {
      false
    } else {
      val that = p1.asInstanceOf[Sample]

      this.id == that.id && this.name == that.name
    }
  }
}