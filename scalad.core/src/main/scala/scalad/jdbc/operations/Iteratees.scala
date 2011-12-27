package scalad.jdbc.operations

import java.sql.{ResultSet, PreparedStatement}
import scalad.PreparedQuery
import scalad.jdbc.{ExecutionPolicy, JDBCOperations}

/**
 * @author janmachacek
 */
trait Iteratees extends ParameterSetter {
  this: JDBCOperations with ExecutionPolicy =>

  import scalaz.Enumerator
  import scalaz.IterV._
  import scalaz.IterV

  class ResultSetIterator[T : ClassManifest](private val resultSet: ResultSet,
                             private val mapper: ResultSetMapper[T]) extends Iterator[T] {

    def hasNext = !resultSet.isLast

    def next() = {
      resultSet.next()
      mapper(resultSet)
    }
    
    def close() {
      println("closed")
      resultSet.close()
    }
  }

  implicit val resultSetEnumerator = new Enumerator[ResultSetIterator] {

    @scala.annotation.tailrec
    def apply[E, A](iterator: ResultSetIterator[E], i: IterV[E, A]): IterV[E, A] = i match {
      case _ if !iterator.hasNext =>
        iterator.close()
        i
      case Done(acc, input) =>
        iterator.close()
        i
      case Cont(k) =>
        val x = iterator.next()
        apply(iterator, k(El(x)))
    }
  }

  def select[R, T : ClassManifest](query: PreparedQuery, i: IterV[T, R])(mapper: ResultSetMapper[T]) = exec {
    val executor = (ps: PreparedStatement) => {
      val rs: ResultSet = ps.executeQuery()
      val iterator = new ResultSetIterator[T](rs, mapper)
      i(iterator).run
    }

    perform[PreparedStatement, R](_.prepareStatement(query.query), parameterSetter(query), executor)
  }

}