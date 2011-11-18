package scalad

/**
 * @author janmachacek
 */
object QuerySpec {
  import Scalad._
  
  def main(args: Array[String]) {
    val simple =   "foo" ＝ "a"
    val and    = (("foo" ＝ "a") && ("bar" like "c")) || ("x" ＝ "y")
    val or     =  ("foo" ＝ "a") || ("bar" like "c")
    val neg    =  ("foo" ＝ "b")
    val gt     =   "foo" > 5
    
    
    println(and)
  }

}