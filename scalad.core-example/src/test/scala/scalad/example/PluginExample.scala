package scalad.example

import org.specs2.mutable.Specification

/**
 * @author janmachacek
 */
class PluginExample extends Specification {
  
  "scalad selector" in {
    println(LensedUser.id)

    success
  }

}