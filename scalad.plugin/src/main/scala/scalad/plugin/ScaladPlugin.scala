package scalad.plugin

import scala.tools.nsc
import nsc.Global
import nsc.plugins.{Plugin, PluginComponent}

class ScaladPlugin(val global: Global) extends Plugin {
  import global._


  val name = "scalad"
  val description = "support for the @selectable annotation"

  val components = List[PluginComponent](
    new GenerateSynthetics(this, global)
  )

}
