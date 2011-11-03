package scalad

/**
 * @author janmachacek
 */
object Scalad {

  import annotation.tailrec
  import scalaz.Input
  import scalaz.IterV._
  import scalaz.Enumerator
  import scalaz.IterV

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

  def one[E]: IterV[E, E] = {
    def step(s: Input[E]): IterV[E, E] =
      s(el = {e =>
          s(el = _ => throw new RuntimeException("Too many results."),
            empty = Done(None, EOF[E]),
            eof = Done(None, EOF[E]))
          Done(e, s)},
        empty = throw new RuntimeException("No results."),
        eof = throw new RuntimeException("No results."))
    Cont(step)
  }


  implicit def toQueryBuilder(s: String) = new QueryBuilder(s, None)

}
