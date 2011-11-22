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
    import CODE._

    def shouldGenerate(sym: Symbol) = sym.isCaseClass && sym.annotations.exists(_.atp.typeSymbol == annotationClass)

    def shouldGenerateForMember(tree: Tree) = {
      val sym = tree.symbol
      sym.isCaseAccessor && sym.isParamAccessor && sym.isMethod
    }

    override def transform(tree: Tree): Tree = {
      val newTree = tree match {
        case cd @ ClassDef(_, _, _, _) if shouldGenerate(cd.symbol) =>
          //          log("found case class. classdef.symbol.tpe: " + cd.symbol.tpe)
          //          log("case class impl: classdef.tpe"+ cd.tpe)
          caseClasses +=  cd.symbol -> cd
          cd
        case _ => tree
      }
      super.transform(newTree)
    }

  }


  val caseClasses = collection.mutable.HashMap[global.Symbol, ClassDef]()
  val annotationClass = definitions.getClass("scalad.annotation.selectable")
  val runsAfter = List("typer")
  val phaseName = "generatesynthetics"
}
