package scalad.jdbc

import javax.sql.DataSource
import scalad.PersistableLike
import java.sql.Connection

class MappingJDBC(dataSource: DataSource) extends JDBC(dataSource) with PersistableLike {
  this: InsertOrUpdateVoter with Inserter with Updater with Deleter with ExecutionPolicy =>

  def underlyingPersist[E](entity: E) {
    withConnection(if (isInsert(entity)) insert(entity, _) else update(entity, _))
  }

  def underlyingDelete[E](entity: E) {
    withConnection(delete(entity, _))
  }

/*
  override def apply(entity: Any) = new MappingExecutor(entity)

  class MappingExecutor(entity: Any) extends Executor(entity) {

    def persist = {
      underlyingPersist(entity)
      entity
    }

    def delete = {
      underlyingDelete(entity)
      entity
    }
  }
   */

}

trait InsertOrUpdateVoter {

  def isInsert[E](entity: E): Boolean

}

trait Inserter {

  def insert[E](entity: E, connection: Connection)

}

trait Updater {

  def update[E](entity: E, connection: Connection)

}

trait Deleter {

  def delete[E](entity: E, connection: Connection)

}