package controllers

import java.util.Date

import models.TokenInfo
import play.Logger
import play.api.libs.json.{JsObject, Json, JsValue}
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
   * @return auth code if success, otherwise failed status or service unavailable
   */
  def generateAuthCode = Action { request =>
    var retObj: JsObject = null
    try {
      val json: Option[JsValue] = request.body.asJson
      val expiredTime: Long = (json.getOrElse(null) \ "expired-time").asOpt[Long].getOrElse(-1)
      // generate id
      if (expiredTime < 0) {
        Logger.error("Malformed request: %s" format json.toString)
        retObj = FailedStatus
      } else {
        val authCode: String = idGenerator.generateAuthCode()
        val curTime = System.currentTimeMillis()
        if (curTime > expiredTime) {
          retObj = FailedStatus
        } else {
          auth.addNewCode(authCode, new Date(curTime), new Date(expiredTime), true, expiredTime - curTime)
          retObj = Json.obj("auth-code" -> authCode)
        }
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("Service is currently unavailable")
      }
    }
  }

  /**
   * Validate auth code. The method will check if the code exists or not first,
   * and then validate the time
   * @return Ok status if success, failed status or service unavailable otherwise
   */
  def validateAuthCode = Action { request =>
    var retObj: JsObject = FailedStatus
    try {
      val json: Option[JsValue] = request.body.asJson
      val code: String = (json.getOrElse(null) \ "auth-code").asOpt[String].getOrElse("")
      if (!code.equals("")) {
        val token: TokenInfo = auth.getAuthCode(code)
        val curTime = System.currentTimeMillis()
        if (token != null) {
          if (token.expiredTime.getTime >= curTime && token.createdTime.getTime <= curTime) {
            retObj = OkStatus
          }
        } else {
          Logger.warn("Validate a expired token: %s" format code)
          retObj = FailedStatus
        }
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("Service is currently unavailable")
      }
      }
  }

  /**
   * Clear auth code by set expired time to current time
   * @param code
   * @return
   */
  def clearToken(code: String) = Action {
    var retObj: JsObject = null
    try {
      if (auth.updateExpiredTime(code, new Date(System.currentTimeMillis()))) {
        retObj = OkStatus
      } else {
        retObj = FailedStatus
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
          Logger.error("exception = %s" format e)
          ServiceUnavailable("Service is currently unavailable")
      }
    }
  }
}
