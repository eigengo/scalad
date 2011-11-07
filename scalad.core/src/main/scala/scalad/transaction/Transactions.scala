package scalad.transaction

/**
 * @author janmachacek
 */
trait Transactions {

  def getPlatformTransactionManager: PlatformTransactionManager

}
