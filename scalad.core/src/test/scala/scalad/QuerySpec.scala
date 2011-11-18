package scalad

/**
 * @author janmachacek
 */
object QuerySpec {
  import Scalad._
  
  def main(args: Array[String]) {
    val simple =   "foo" ＝ "a"
    val and    = (("foo" ＝ "a") && ("bar" like "c")) || ("x" ＝ "y") orderBy("foo" desc, "bar" asc) groupBy "bar"
    val or     =  ("foo" ＝ "a") || ("bar" like "c")
    val neg    =  ("foo" ＝ "b")
    val gt     =   "foo" > 5 orderBy("foo" asc, "bar" desc) groupBy("xxx")
    val ident  =   id ＝ 6

    println(and)
    println(gt)
    println(ident)
  }

}