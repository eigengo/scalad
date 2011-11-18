package scalad

/**
 * @author janmachacek
 */
class Query private (val restrictions: Seq[Restriction]) {

  def &&(q: Query) = this

  def ||(q: Query) = this

}

abstract class Restriction
final case class Eq(property: String, value: Option[Any]) extends Restriction
final case class Like(property: String, value: Option[Any]) extends Restriction
final case class NotEq(property: String, value: Option[Any]) extends Restriction
final case class GT(property: String, value: Option[Any]) extends Restriction
final case class LT(property: String, value: Option[Any]) extends Restriction
final case class GTE(property: String, value: Option[Any]) extends Restriction
final case class LTE(property: String, value: Option[Any]) extends Restriction