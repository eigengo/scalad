package scalad

import java.lang.reflect.{Method, Field}

/**
 * @author janmachacek
 */
abstract class Selectable
final case class SelectableField(field: Field) extends Selectable
final case class SelectableSetter(method: Method) extends Selectable