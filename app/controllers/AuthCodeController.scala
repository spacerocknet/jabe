package controllers

import java.util.Date

import models.TokenInfo
import play.Logger
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, Controller}
import scaldi.{Injector, Injectable}
import spacerock.persistence.cassandra.AuthCode
import spacerock.utils.IdGenerator

/**
 * Created by william on 3/4/15.
 */
class AuthCodeController (implicit inj: Injector) extends Controller with Injectable {
  val auth = inject[AuthCode]
  val idGenerator = inject[IdGenerator]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")

  /**
   * Serve for generating auth token, insert into database and send the token back to client
   * @return auth code if success, otherwise false
   */
  def generateAuthCode = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
//      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val expiredTime: Long = (json.getOrElse(null) \ "expired-time").asOpt[Long].getOrElse(-1)
      // generate id
//      if (uid == null || uid.equals("") || expiredTime < 0) {
      if (expiredTime < 0) {
        Logger.error("Malformed request: %s" format json.toString)
        Ok(FailedStatus)
      } else {
        val authCode: String = idGenerator.generateAuthCode()
        val curTime = System.currentTimeMillis()
        if (curTime > expiredTime) {
          Ok(FailedStatus)
        } else {
          auth.addNewCode(authCode, new Date(curTime), new Date(expiredTime), true, expiredTime - curTime)
          Ok(Json.obj("auth-code" -> authCode))
        }
      }
    } catch {
      case e: Exception => {Logger.error("exception = %s" format e)}
    }
    ServiceUnavailable("Service is currently unavailable")
  }

  /**
   * Validate auth code. The method will check if the code exists or not first,
   * and then validate the time
   * @param code auth code to check
   * @return Ok status if success, failed status or service unavailable otherwise
   */
  def validateAuthCode(code: String) = Action {
    try {
      val token: TokenInfo = auth.getAuthCode(code)
      val curTime = System.currentTimeMillis()
      if (token != null) {
        if (token.expiredTime.getTime >= curTime && token.createdTime.getTime <= curTime) {
          Ok(OkStatus)
        }
      }
      Ok(FailedStatus)
    } catch {
      case e: Exception => Logger.error("exception = %s" format e)
    }
    ServiceUnavailable("Service is currently unavailable")
  }

  /**
   * Clear auth code by set expired time to current time
   * @param code
   * @return
   */
  def clearToken(code: String) = Action {
    try {
      if (auth.updateExpiredTime(code, new Date(System.currentTimeMillis()))) {
        Ok(OkStatus)
      } else {
        Ok(FailedStatus)
      }
    } catch {
      case e: Exception => {Logger.error("exception = %s" format e)}
    }
    ServiceUnavailable("Service is currently unavailable")
  }
}
