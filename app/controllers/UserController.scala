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
    val result: List[Subscriber] = userDao.getAllUsers()
    var seq = Seq[JsObject]()
    for(subscriber <- result) {

      val userString = Json.obj(
        "uuid" -> (if (subscriber.uid == null) "" else subscriber.uid),
        "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
        "os" -> (if (subscriber.os == null) "" else subscriber.os),
        "model" -> (if (subscriber.model == null) "" else subscriber.model),
        "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
        "email" -> (if (subscriber.email == null) "" else subscriber.email),
        "fbId" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
        "state" -> (if (subscriber.state == null) "" else subscriber.state),
        "region" -> (if (subscriber.region == null) "" else subscriber.region),
        "country" -> (if (subscriber.country == null) "" else subscriber.country),
        "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))

      seq = seq:+ userString
    }
    Ok(JsArray(seq))
	}

  // get user by uid
  def getUserInfoByUID (uid: String) = Action {
    try {
      val subscriber: Subscriber = userDao.getUserInfoByUID(uid)
      if (subscriber != null) {
        val userString = Json.obj(
          "uuid" -> (if (subscriber.uid == null) "" else subscriber.uid),
          "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
          "os" -> (if (subscriber.os == null) "" else subscriber.os),
          "model" -> (if (subscriber.model == null) "" else subscriber.model),
          "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
          "email" -> (if (subscriber.email == null) "" else subscriber.email),
          "fbId" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
          "state" -> (if (subscriber.state == null) "" else subscriber.state),
          "region" -> (if (subscriber.region == null) "" else subscriber.region),
          "country" -> (if (subscriber.country == null) "" else subscriber.country),
          "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))

        Ok(userString)
      } else {
        Ok("{}")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  // get user by username
  def getUserInfoByUsername (userName: String) = Action {
    try {
      val subscriber: Subscriber = userDao.getUserInfoByUsername(userName)
      if (subscriber != null) {
        val userString = Json.obj(
          "uuid" -> (if (subscriber.uid == null) "" else subscriber.uid),
          "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
          "os" -> (if (subscriber.os == null) "" else subscriber.os),
          "model" -> (if (subscriber.model == null) "" else subscriber.model),
          "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
          "email" -> (if (subscriber.email == null) "" else subscriber.email),
          "fbId" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
          "state" -> (if (subscriber.state == null) "" else subscriber.state),
          "region" -> (if (subscriber.region == null) "" else subscriber.region),
          "country" -> (if (subscriber.country == null) "" else subscriber.country),
          "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))

        Ok(userString)
      } else {
        Ok("{}")
      }
    } catch {
      case e: Exception => {
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  // add new user
  def updateUserInfo = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse(null)
      val userName = (json.getOrElse(null) \ "user-name").asOpt[String].getOrElse(null)
      val firstName = (json.getOrElse(null) \ "first-name").asOpt[String].getOrElse("")
      val lastName = (json.getOrElse(null) \ "last-name").asOpt[String].getOrElse("")
      val email = (json.getOrElse(null) \ "email").asOpt[String].getOrElse("")
      val fbId = (json.getOrElse(null) \ "fb-id").asOpt[String].getOrElse("")
      val locState = (json.getOrElse(null) \ "state").asOpt[String].getOrElse("")
      val locRegion = (json.getOrElse(null) \ "region").asOpt[String].getOrElse("")
      val locCountry = (json.getOrElse(null) \ "country").asOpt[String].getOrElse("")
      val appName = (json.getOrElse(null) \ "apps").asOpt[String].getOrElse("")
      if (uid != null) {
        //generate uuid

        userDao.addUserBasicInfo(uid, userName, firstName, lastName, email, fbId,
                            locState, locRegion, locCountry, appName)
        val userString = Json.obj(
          "uuid" -> uid)
        Ok(userString)
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
    try {
      val json: Option[JsValue] = request.body.asJson

      val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
      val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
      val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
      val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
      val deviceUuid = (json.getOrElse(null) \ "device-uuid").asOpt[String].getOrElse("")
      val uuid = uuidGenerator.generate
      val subscriber = new Subscriber(uuid, platform, os, model, phone, deviceUuid)
      val status: Boolean = userDao.addDeviceInfo(subscriber)
      if (status) {
        val userString = Json.obj("uuid" -> uuid)
        Ok(userString)
      } else {
        ServiceUnavailable("Service is currently unavailable")
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Internal server error")
      }
    }
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