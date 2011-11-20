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
    val at     =  ("foo" ＝ "a") || ("foo" !＝ "a")
    val af     =  ("foo" ＝ "a") && ("foo" !＝ "a")
    val af3    =  ("foo" ＝ "a") && ("foo" ＝ "b")
    val at2    =  ("foo" ＝ "a") || (("foo" !＝ "a") || ("foo" ＝ "a"))
    val af2    =  ("foo" ＝ "a") && (("foo" !＝ "a") && ("foo" ＝ "a"))
    val si1    =  ("foo" ＝ "a") && (("foo" ＝ "a") && ("foo" ＝ "a"))

    println(and)
    println(gt)
    println(ident)

    val v      = "X"
    val simpl  = (id ＝ 5) || ("X" ＝ v when v != "X") 

    println("***")

    val querySimplifier: QuerySimplifier = new QuerySimplifier()
    println(querySimplifier.simplify(simpl))
    println(querySimplifier.simplify(at))
    println(querySimplifier.simplify(at2))
    println(querySimplifier.simplify(af2))
    println(querySimplifier.simplify(af))
    println(querySimplifier.simplify(af3))
    println(querySimplifier.simplify(si1))
  }

}