package controllers

import models.Subscriber
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Controller
import scaldi.Injectable
import scaldi.Injector
import spacerock.persistence.UserDataDAO
import spacerock.utils.UuidGenerator



class SubscriberController(implicit inj: Injector) extends Controller with Injectable {
  val subscriberDataDao = inject [UserDataDAO]
  val uuidGenerator = inject [UuidGenerator]
  
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")
  
  /* generate uuid for client */
  def generateUniqId() = Action {
      val json = Json.obj("uuid" -> uuidGenerator.generate())
      Ok(json)
  }
  
  /*
   * Register with only UUID
   */
  def noInfoRegister = Action { request =>
     var result = OkStatus
     println(request);
     println(request.headers)
     println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
     
     try {
       val json: Option[JsValue] = request.body.asJson
       println("Body ::: ")
       println(request.body)
       println(json)
       val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
       val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
       val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
       val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
       val deviceUuid = (json.getOrElse(null) \ "deviceUuid").asOpt[String].getOrElse("")

       var subscriber = new Subscriber(uuidGenerator.generate, platform, os, model, phone, deviceUuid)
       val status = subscriberDataDao.insertNewUserWithNoInfo(subscriber)
       if (!status) {
          result = FailedStatus 
       }
    } catch {
       //case e:IllegalArgumentException => BadRequest("Product not found")
       case e:Exception => {
         Logger.info("exception = %s" format e)
         BadRequest("Internal server error")
       }
    }
    Ok(result)
  }
  
  
  /*
   *  Register with FB info.  Payload contains FB info
   *  Return: user id
   */
  def fbRegister() = Action {
      //Subscriber.connect("localhost")
      //val userDataDao = new UserDataDAO
      //userDataDao.insertRandom
      subscriberDataDao.insertRandom
      Ok("user_id")
  }
  
  /*
   * login to begin to play game(s)
   * Return: game session id
   */
  def login(uid : Long) = Action {
      Ok("game_session_id")
  }
  
  /* Return all info about user
   * 
   */
  def getUserProfile() = Action { request =>
     var result = OkStatus
     println(request);
     println(request.headers)
     println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
     
     try {
       val json: Option[JsValue] = request.body.asJson
       println("Body ::: ")
       println(request.body)
       println(json)
       val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("") //.map(_.toString).getOrElse("default")
       
       val user: Subscriber = subscriberDataDao.retrieveUser(uid)
       if (user != null) {
          val json = Json.obj(
                  "uid" -> user.uuid,
                  "os" -> user.os,
                  "platform" -> user.platform,
                  "model" -> user.model,
                  "phone" -> user.phone,
                  "deviceUuid" -> user.deviceUuid,
                  "status" -> "found"
                 )
          Ok(json)
       } else {
          val json = Json.obj("status" -> "unknown")
          Ok(json)
       }
    } catch {
       //case e:IllegalArgumentException => BadRequest("Product not found")
       case e:Exception => {
         Logger.info("exception = %s" format e)
         BadRequest("Internal server error")
       }
    } 
  }
}
