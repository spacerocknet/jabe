package controllers


import java.util.concurrent.atomic.AtomicLong

import scala.util.Random

import play.api.mvc._
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

import scaldi.Injector
import scaldi.Injectable

import spacerock.persistence.UserDataDAO
import spacerock.utils.UuidGenerator
import models.QuAn
import models.Subscriber



class SubscriberController(implicit inj: Injector) extends Controller with Injectable {
  val subscriberDataDao = inject [UserDataDAO]
  val uuidGenerator = inject [UuidGenerator]
  
  /* generate uuid for client */
  def generateUniqId() = Action {
      val json = Json.obj("uuid" -> uuidGenerator.generate())
      Ok(json)
  }
  
  
  implicit val subscriberReads: Reads[Subscriber] = (
    (JsPath \ "uuid").read[String] and  
    (JsPath \ "platform").read[String] and
    (JsPath \ "os").read[String] and
    (JsPath \ "model").read[String] and
    (JsPath \ "phone").read[String] and
    (JsPath \ "deviceUuid").read[String]
  )(Subscriber.apply _)
  
  /*
   * Register with only UUID
   */
  def noInfoRegister = Action(parse.json) { request =>
     println(request);
     println(request.headers)
     println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
     subscriberDataDao.insertNewUser(uuidGenerator.generate())
     Ok("Ok")
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
  
  
}
