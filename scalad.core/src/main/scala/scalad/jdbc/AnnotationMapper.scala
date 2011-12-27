package scalad.jdbc

import java.sql.ResultSet

/**
 * @author janmachacek
 */
trait AnnotationMapper {

  def mapper[T](implicit evidence: ClassManifest[T]) = {rs: ResultSet =>
      evidence.erasure.newInstance().asInstanceOf[T]
  }

}

