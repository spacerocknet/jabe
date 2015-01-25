package controllers

import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence.NewUserData
import spacerock.utils.UuidGenerator

class UserController (implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [NewUserData]
  val uuidGenerator = inject [UuidGenerator]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")

  /* generate uuid for client */
  def generateUniqId() = Action {
      val json = Json.obj("uuid" -> uuidGenerator.generate())
      Ok(json)
  }

  // get all user from database
	def getAllUser = Action {
	    val s: List[Subscriber] = userDao.getAllUsers()
	    println(s)
	    Ok(s.toString)
	}

  // get user by uid
  def getUserInfoByUID = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val uidString = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse(null)
      if (uidString != null) {
        Ok(userDao.getUserInfoByUID(uidString).toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  // get user by username
  def getUserInfoByUsername = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val userName = (json.getOrElse(null) \ "username").asOpt[String].getOrElse(null)
      if (userName != null) {

        Ok(userDao.getUserInfoByUsername(userName).toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  // add new user
  def addUser = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val userName = (json.getOrElse(null) \ "username").asOpt[String].getOrElse(null)
      val firstName = (json.getOrElse(null) \ "firstname").asOpt[String].getOrElse("")
      val lastName = (json.getOrElse(null) \ "lastname").asOpt[String].getOrElse("")
      val email = (json.getOrElse(null) \ "email").asOpt[String].getOrElse("")
      val fbId = (json.getOrElse(null) \ "fbid").asOpt[String].getOrElse("")
      val locState = (json.getOrElse(null) \ "locstate").asOpt[String].getOrElse("")
      val locRegion = (json.getOrElse(null) \ "locregion").asOpt[String].getOrElse("")
      val appName = (json.getOrElse(null) \ "appname").asOpt[String].getOrElse("")
      if (userName != null) {
        //generate uuid
        val uid: String = uuidGenerator.generate()

        userDao.addUserBasicInfo(uid, userName, firstName, lastName, email, fbId,
                            locState, locRegion, appName)
        Ok("\"uid\":\"" + uid.toString + "\"")
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  // add new user without user's information
  def addUserWithoutInfo = Action { request =>
    var result = OkStatus

    try {
      val json: Option[JsValue] = request.body.asJson

      val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
      val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
      val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
      val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
      val deviceUuid = (json.getOrElse(null) \ "deviceUuid").asOpt[String].getOrElse("")

      val subscriber = new Subscriber(uuidGenerator.generate, platform, os, model, phone, deviceUuid)
      val status: Boolean = userDao.addDeviceInfo(subscriber)
      if (status) {
        result = FailedStatus
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Internal server error")
      }
    }
    Ok(result)
  }

  // add new user without user's information
  def userChangeDevice = Action { request =>
    var result = OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].orNull
      val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
      val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
      val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
      val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
      val deviceUuid = (json.getOrElse(null) \ "deviceUuid").asOpt[String].getOrElse("")

      if (uid == null) {
        BadRequest("Malformed request")
      } else {
        val subscriber = new Subscriber(uuidGenerator.generate, platform, os, model, phone, deviceUuid)
        val status: Boolean = userDao.addDeviceInfo(subscriber)
        if (!status) {
          result = FailedStatus
        }
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Internal server error")
      }
    }
    Ok(result)

  }
}