package scalad.plugin

import scala.tools._
import nsc.Global
import nsc.plugins.PluginComponent
import nsc.transform.{Transform, TypingTransformers}
import nsc.symtab.Flags._
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

    def shouldGenerate(sym: Symbol) = (sym.isClass || sym.isCaseClass) && sym.annotations.exists(_.atp.typeSymbol == annotationClass)

    def shouldGenerateForMember(tree: Tree) = {
      val sym = tree.symbol
      sym.isGetter //|| sym.isVariable
      // sym.isCaseAccessor && sym.isParamAccessor && sym.isMethod
    }

    def generateSelectors(moduleClass: Symbol, caseClass: Symbol, caseClassTypeParams: List[Symbol], members: List[Tree], defaultMemberOrder: List[Tree]): List[Tree] = {
      members.map { member =>
        val memberSym = member.symbol

        println("****** Generate selector for " + member)
        
        val referenceDefSym = moduleClass.newMethod(moduleClass.pos.focus, memberSym.name.toTermName)
        referenceDefSym setFlag (SYNTHETIC)
        moduleClass.info.decls enter referenceDefSym

        // DEF(referenceDefSym) === rhs
      }
      List()
    }

    private def generateCompanionObject(cd: ClassDef) {
      val selectorMembers = cd.impl.body.filter(shouldGenerateForMember _)
      selectorMembers.foreach { member =>
        val memberSym = member.symbol
        
        
        
        println("****** Generate selector for " + member)
      }
    }
    override def transform(tree: Tree): Tree = {
      val newTree = tree match {
        case cd @ ClassDef(_, _, _, _) if shouldGenerate(cd.symbol) =>
          generateCompanionObject(cd)
          cd
        case _ => tree
      }
      super.transform(newTree)
    }

  }

  val annotationClass = definitions.getClass("scalad.annotation.selectable")
  val selectableSetterClass = definitions.getClass("scalad.SelectableSetter")
  val runsAfter = List("typer")
  val phaseName = "generatesynthetics"
}
