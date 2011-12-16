package scalad.jdbc

import java.sql.Connection
import javax.persistence.{Table, Column, Id}
import java.lang.reflect.{AccessibleObject, Method, Field, Member}

/**
 * @author janmachacek
 */
trait AnnotationSQL extends InsertOrUpdateVoter with Inserter with Updater with Deleter {
  
  private[AnnotationSQL] class Reflector[E](private val o: E) {
    private val c: Class[_] = o.getClass
    
    def getMembers(f: (AccessibleObject) => Boolean) = {
      c.getDeclaredFields.filter(f) ++ c.getDeclaredMethods.filter(f)
    }
    
    def getAnnotation[A <: java.lang.annotation.Annotation](clazz: Class[A]) = {
      val annotation: A = c.getAnnotation(clazz)
      if (annotation == null) None else Some(annotation)
    }
    
    def simpleName = c.getSimpleName
    
    def getMember(f: (AccessibleObject) => Boolean) = getMembers(f).find(f)
    
    def get(member: Member) = member match {
      case f: Field =>
        f.setAccessible(true)
        f.get(o)
      case m: Method => 
        m.setAccessible(true)
        m.invoke(o)
    }
  }
  
  def delete[E](entity: E, connection: Connection) {
    val r = new Reflector(entity)
    
    val (columnName, columnValue) = r.getMember(_.isAnnotationPresent(classOf[Id])) match {
      case Some(member) =>
        val column = member.getAnnotation(classOf[Column])
        val columnName = if (column != null) column.name() else "id"
        val columnValue = r.get(member)
        
        (columnName, columnValue)
      case _ => throw new RuntimeException("Cannot find the @Id annotation on " + entity)
    }
    
    val (table, schema) = r.getAnnotation(classOf[Table]) match {
        case Some(annotation) => (annotation.name(), annotation.schema())
        case _ => (r.simpleName.toUpperCase, "")
      }
   

    println("DELETE FROM " + table + " WHERE " + columnName + " = " + columnValue)
  }

  def update[E](entity: E, connection: Connection) {
    println("SQL: UPDATE using JPA from " + entity)
  }

  def insert[E](entity: E, connection: Connection) {
    println("SQL: INSERT using JPA from " + entity)
  }

  def isInsert[E](entity: E) = true
}