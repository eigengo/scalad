package scalad

/**
 * @author janmachacek
 */
class QueryBuilder(val property: String, val value: Option[AnyRef]) {

  def is(value: AnyRef) = new QueryBuilder(property, Some(value))

  def like(value: AnyRef) = new QueryBuilder(property, Some(value))

  def && = this

}
