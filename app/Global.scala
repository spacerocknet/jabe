
import play.api._
import play.api.GlobalSettings
import scaldi.play.ScaldiSupport

import modules.{WebModule, UserModule}

object Global extends GlobalSettings with ScaldiSupport {
  def applicationModule = new WebModule :: new UserModule
  
}
