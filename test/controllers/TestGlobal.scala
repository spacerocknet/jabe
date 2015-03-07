package controllers

import modules.{UserModule, WebModule}
import play.api.GlobalSettings
import scaldi.play.ScaldiSupport

/**
 * Created by william on 3/7/15.
 */
object TestGlobal extends GlobalSettings with ScaldiSupport {
  def applicationModule = new WebModule :: new UserModule
}
