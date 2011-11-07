package scalad.transaction

/**
 * @author janmachacek
 */
trait PlatformTransaction {

  def begin()

  def rollback()

  def commit()

}