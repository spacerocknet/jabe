package controllers

import java.util.Date

import models.TokenInfo
import play.Logger
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc.{Action, Controller}
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants
import spacerock.persistence.cassandra.AuthCode
import spacerock.utils.{StaticVariables, IdGenerator}

/**
 * Created by william on 3/4/15.
 */
class AuthCodeController (implicit inj: Injector) extends Controller with Injectable {
  val auth = inject[AuthCode]
  val idGenerator = inject[IdGenerator]


  /**
   * Serve for generating auth token, insert into database and send the token back to client
   * @return auth code if success, otherwise @StaticVariables.BackendErrorStatus or @StaticVariables.WrongInputValueStatus
   */
  def generateAuthCode = Action { request =>
    var retObj: JsObject = null
    try {
      val json: Option[JsValue] = request.body.asJson
      val expiredTime: Long = (json.getOrElse(null) \ "expired_time").asOpt[Long].getOrElse(-1)
      // generate id
      if (expiredTime < 0) {
        Logger.error("Malformed request: %s" format json.toString)
        retObj = StaticVariables.WrongInputValueStatus
      } else {
        val authCode: String = idGenerator.generateAuthCode()
        val curTime = System.currentTimeMillis()
        if (curTime > expiredTime) {
          retObj = StaticVariables.WrongInputValueStatus
        } else {
          auth.addNewCode(authCode, new Date(curTime), new Date(expiredTime), true, expiredTime - curTime)
          retObj = Json.obj("auth_code" -> authCode)
        }
      }

    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }

  /**
   * Validate auth code. The method will check if the code exists or not first,
   * and then validate the time
   * @return Ok status if success, failed status or service unavailable otherwise
   */
  def validateAuthCode = Action { request =>
    var retObj: JsObject = null
    try {
      val json: Option[JsValue] = request.body.asJson
      val code: String = (json.getOrElse(null) \ "auth_code").asOpt[String].getOrElse("")
      if (!code.equals("")) {
        val token: TokenInfo = auth.getAuthCode(code)
        val curTime = System.currentTimeMillis()
        if (token != null) {
          if (token.expiredTime.getTime >= curTime && token.createdTime.getTime <= curTime) {
            retObj = StaticVariables.OkStatus
          }
        } else {
          Logger.warn("Validate a expired token: %s" format code)
          retObj = StaticVariables.WrongInputValueStatus
        }
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
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
        retObj = StaticVariables.OkStatus
      } else {
        retObj = StaticVariables.DbErrorStatus
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }
}
