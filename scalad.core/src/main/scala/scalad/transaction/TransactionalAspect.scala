package scalad.transaction

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Pointcut, Aspect}
import scalad.Scalad


/**
 * @author janmachacek
 */
@Aspect
class TransactionalAspect {

  @Pointcut("execution(* *(..))")
  private def anyExecution() {}

  @Pointcut("this(transactional)")
  private def transactionalTrait(transactional: Transactions) {}

  @Around(value = "anyExecution() && transactionalTrait(transactional)")
  def transactionally(pjp: ProceedingJoinPoint, transactional: Transactions) = {
    Scalad.transactionally(transactional.getPlatformTransactionManager) {
      pjp.proceed()
    }
  }

}