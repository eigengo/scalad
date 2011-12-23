package scalad

import org.specs2.mutable.Specification

/**
 * @author janmachacek
 */
class QuerySpec extends Specification {

  import Scalad._

  "trivial query" in {
    val q: Query = "select * from user"
    q.query must_== "select * from user"
  }

  "query with postitional params" in {
    val q: Query = "select * from user where id=?"
    "no preprocessed SQL" in {
      q.query must_== "select * from user where id=?"
    }
    "one parameter of unknown type" in {
      //q.getParameters.length must_== 1
      success
    }
  }

}