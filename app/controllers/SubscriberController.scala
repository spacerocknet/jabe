package controllers


import scala.util.Random
import play.api.mvc._
import play.api.mvc.Controller
import models.Subscriber
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger
import com.spacerock.persistence.UserDataDAO


object SubscriberController extends Controller {


  /*
   *  Register with FB info.  Payload contains FB info
   *  Return: user id
   */
  def fbRegister() = Action {
       //Subscriber.connect("localhost")
       val userDataDao = new UserDataDAO
       userDataDao.insertRandom
       
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
