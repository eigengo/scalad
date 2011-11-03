package org.cakesolutions.scaladata

import annotation.tailrec
import scalaz.{IterV, Enumerator}
import scalaz.IterV.{Cont, Done}
import scalaz.IterV.El._

/**
 * @author janmachacek
 */
object ScalaData {

  import scalaz.IterV._
  import scalaz._

  implicit val javaIteratorEnumerator = new Enumerator[java.util.Iterator] {

    @tailrec
    def apply[E, A](iterator: java.util.Iterator[E], i: IterV[E, A]): IterV[E, A] = i match {
      case _ if !iterator.hasNext => i
      case Done(acc, input) => i
      case Cont(k) =>
        val x = iterator.next
        apply(iterator, k(El(x)))
    }
  }

  def list[T] = collect[T, List]

  implicit def toQueryBuilder(s: String) = new QueryBuilder(s, None)

}


class QueryBuilder(val property: String, val value: Option[AnyRef]) {

  def is(value: AnyRef) = new QueryBuilder(property, Some(value))

  def like(value: AnyRef) = new QueryBuilder(property, Some(value))

  def && = this

}

class Query {

  def apply(a: Int) = a

}
