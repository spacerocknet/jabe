package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.GlobalSettings
import scaldi.play.ScaldiSupport
import scaldi.Module
import modules.{WebModule, UserModule}

/**
 * Created by william on 3/5/15.
 */

object TestGlobal extends GlobalSettings with ScaldiSupport {
  // test module will override `MessageService`
  def applicationModule = new WebModule :: new UserModule
}

object TestAuthCodeController extends Specification {

  def main(args: Array[String]): Unit = {
    running(FakeApplication(withGlobal = Some(TestGlobal))) {
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      println(contentAsString(home))
      contentAsString(home) must contain ("Test Message")
    }
  }
}


