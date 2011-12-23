package scalad

/**
 * @author janmachacek
 */
class ExecutableQuery(rawQuery: Query)(implicit simplifier: RestrictionSimplifier) {
  
  def query = rawQuery.query
  
  def restriction = simplifier.simplifyRestriction(rawQuery.restriction)

  def getParameters = {

  }

}