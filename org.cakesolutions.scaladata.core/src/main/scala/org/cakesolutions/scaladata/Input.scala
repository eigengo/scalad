package org.cakesolutions.scaladata


object NoSQL {

  import scalaz._
  import Scalaz._
  import IterV._

  implicit val StreamEnumerator = new Enumerator[Stream] {
    def apply[E, A](e: Stream[E], i: IterV[E, A]): IterV[E, A] = e match {
      case Stream() => i
      case x #:: xs => i.fold(done = (_, _) => i, cont = k => apply(xs, k(El(x))))
    }
  }

  val list = collect[Int, List]
  val reverse = reversed[Int, List](ListReducer)
  val repeatHead = repeat[Int, Option[Int], List](head)

  def select(query: String) = Stream(1, 2, 3)

  def run {
    val x = head(select("x"))
    println(x.run)
    println(length(Stream(1, 2, 3)).run)

    // As a monad
    val m1 = head[Int] >>= ((b: Option[Int]) => head[Int] map (b2 => (b <|*|> b2)))
    println(m1(Stream(1,2,3)).run)
  }

}