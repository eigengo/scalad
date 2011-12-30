package scalad.jdbc

import com.mchange.v2.c3p0.ComboPooledDataSource
import javax.sql.DataSource
import org.specs2.mutable.Specification

/**
 * @author janmachacek
 */
trait JDBCSpecification extends Specification {
  type JDBCType <: JDBC

  setup

  val instance: JDBCType = setup

  def jdbc(dataSource: DataSource): JDBCType

  private final def setup[T <: JDBCType] = {
    val dataSource = new ComboPooledDataSource();
    dataSource.setDriverClass("org.hsqldb.jdbc.JDBCDriver")
    dataSource.setJdbcUrl("jdbc:hsqldb:mem:test");
    dataSource.setUser("sa");
    
    jdbc(dataSource)
  }
  
}