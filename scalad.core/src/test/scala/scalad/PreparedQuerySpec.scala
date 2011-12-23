package scalad

import org.specs2.mutable.Specification

/**
 * @author janmachacek
 */
class PreparedQuerySpec extends Specification {

  import Scalad._

  "trivial query" in {
    val q: PreparedQuery = "select * from user"
    q.query must_== "select * from user"
  }

  "query with postitional params only" in {
    val t = "select * from user where id=? or name like ?"
    val q: PreparedQuery = t
    "no preprocessed SQL" in {
      q.query must_== t
    }
    "two positional parameters" in {
      q.parameters must_== List(PositionalPreparedQueryParameter(0), PositionalPreparedQueryParameter(1))
    }
  }

  "query with named & duplicate params only" in {
    val q: PreparedQuery = "select * from user where id=:id or name like :name or name = upper(:name)"
    "replaced named with ?" in {
      q.query must_== "select * from user where id=? or name like ? or name = upper(?)"
    }
    "three named parameters" in {
      q.parameters must_== List(NamedPreparedQueryParameter(":id", 0), NamedPreparedQueryParameter(":name", 1),
                                NamedPreparedQueryParameter(":name", 2))
    }
  }

  "query with property-styled & duplicate named params only" in {
    val q: PreparedQuery = "select * from user where id=:u.id or name like :u.name or name = upper(:u.name)"
    "replaced named with ?" in {
      q.query must_== "select * from user where id=? or name like ? or name = upper(?)"
    }
    "three named parameters" in {
      q.parameters must_== List(NamedPreparedQueryParameter(":u.id", 0), NamedPreparedQueryParameter(":u.name", 1),
                                NamedPreparedQueryParameter(":u.name", 2))
    }
  }

}