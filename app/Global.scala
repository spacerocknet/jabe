import java.net.InetAddress

import modules.{UserModule, WebModule}
import play.api.GlobalSettings
import scaldi.play.ScaldiSupport
import spacerock.utils.{StaticVariables, Utilities}

object Global extends GlobalSettings with ScaldiSupport {
  def applicationModule = new WebModule :: new UserModule
  StaticVariables.serverIp = InetAddress.getByName(inject [String] (identified by "server.ip"))
  StaticVariables.serverIpInt = Utilities.ipToInt(StaticVariables.serverIp)

//  override def onStop(app: App): Unit = {
//
//  }
}
