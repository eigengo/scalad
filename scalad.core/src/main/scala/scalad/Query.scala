package scalad

/**
 * @author janmachacek
 */
class Query(val property: String, val value: Option[AnyRef]) {

  def is(value: AnyRef) = new Query(property, Some(value))

  def like(value: AnyRef) = new Query(property, Some(value))

  def && = this

  def || = this

  def orderBy() = this

}
