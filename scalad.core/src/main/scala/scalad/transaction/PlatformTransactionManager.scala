package scalad.transaction

/**
 * @author janmachacek
 */
trait PlatformTransactionManager {

  def getTransaction: PlatformTransaction

}