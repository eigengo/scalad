package scalad.plugin

import scala.tools._
import nsc.Global
import nsc.plugins.PluginComponent
import nsc.transform.{Transform, TypingTransformers}
import nsc.ast.TreeDSL

class GenerateSynthetics(plugin: ScaladPlugin, val global: Global) extends PluginComponent
  with Transform
  with TypingTransformers
  with TreeDSL {

  import global._
  import definitions._

  protected def newTransformer(unit: CompilationUnit) = new ScaladTransformer(unit)

  class ScaladTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {

  }

  val runsAfter = List("typer")
  val phaseName = "generatesynthetics"
}
