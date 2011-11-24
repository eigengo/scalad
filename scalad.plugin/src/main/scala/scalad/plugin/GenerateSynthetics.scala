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

    private def generateCompanionObject(cd: ClassDef) = {
      val selectorMembers = cd.impl.body.filter(shouldGenerateForMember _)
      val packageSymbol = cd.symbol.owner

      val objectClass = packageSymbol.newClass(packageSymbol.pos.focus, cd.name.toTypeName)
      objectClass setAnnotations Nil
      objectClass setFlag SYNTHETIC
      objectClass setInfo ClassInfoType(ScalaObjectClass.tpe :: Nil, new Scope, objectClass)

      val body = selectorMembers.map { member =>
        val memberSym = member.symbol

        val selector = objectClass.newMethod(objectClass.pos.focus, memberSym.name.toTermName)
        selector setFlag SYNTHETIC
        selector setInfo MethodType(selector.newSyntheticValueParams(Nil), definitions.StringClass.tpe)
        //selector setInfo MethodType(selector.newSyntheticValueParams(List()), selectableSetterClass.typeOfThis)

        objectClass.info.decls enter selector

        DefDef(selector,
          Return(Literal("x"))
        )
      }

      packageSymbol.info.decls enter objectClass

      val template = Template(objectClass.info.parents map TypeTree, emptyValDef, body)
      val md = treeCopy.ModuleDef(cd, NoMods, objectClass.name, template)

      md
    }

    override def transform(tree: Tree): Tree = {
      val newTree = tree match {
        case cd @ ClassDef(mods, name, tparams, impl) if shouldGenerate(cd.symbol) =>
          println(cd.symbol.owner)
          val md = generateCompanionObject(cd)
          println("combine " + cd + " with " + md + "<<<")
          
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
