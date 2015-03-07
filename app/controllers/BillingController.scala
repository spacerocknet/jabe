package controllers

import java.util.Date

import models.{SkuModel, BillingRecordModel}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, Controller}
import scaldi.{Injector, Injectable}
import spacerock.persistence.cassandra._
import spacerock.utils.IdGenerator

/**
 * Created by william on 2/23/15.
 */
class BillingController(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [UserData]
  val idGenerator = inject [IdGenerator]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")
  val uidBlock = inject [UidBlock]
  val idLocker = inject [CassandraLock]
  val billing = inject [Billing]
  val sku = inject [Sku]
  implicit val billFmt = Json.format[BillingRecordModel]

  /**
   * Add new billing info. This method use REST POST method with json body includes: uid, game-id, timestamp, sku-id,
   * number-of-items and discount info.
   * To add new bill, the sku-id will be checked to know if it exists and if it is still valid in system or not first.
   * @return Ok status if success, otherwise bad request, failed status, service unavailable
   */
  def addNewBill() = Action { request =>
    var retObj: JsObject = FailedStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gameId: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(-1)
      val ts: Long = (json.getOrElse(null) \ "timestamp").asOpt[Long].getOrElse(System.currentTimeMillis())
      val skuId: Int = (json.getOrElse(null) \ "sku-id").asOpt[Int].getOrElse(-1)
      val nItems: Int = (json.getOrElse(null) \ "num-items").asOpt[Int].getOrElse(-1)
      val totalDiscount: Float = (json.getOrElse(null) \ "discount").asOpt[Float].getOrElse(0.0f)

      if (uid.equals("") || gameId < 0 || skuId < 0 || nItems < 0) {
        Logger.warn("Invalid request. Some fields are not filled. %s" + json.toString)
      } else {
        // check sku
        val skuModel: SkuModel = sku.getSkuInfo(skuId)
        if(skuModel != null) {
          if (skuModel.expiredTime.getTime >= ts && skuModel.startTime.getTime <= ts) {
            // valid
            if (billing.addNewBill(uid, new Date(ts), gameId, skuId, nItems, totalDiscount)) {
              retObj = OkStatus
            } else {
              Logger.warn("Cannot add new billing record to database. Please check again. %s"
                          format json.toString)
            }
          } else {
            Logger.warn("Request to invalid sku. %s" format json.toString)
          }
        } else {
          Logger.warn("Request to invalid sku. %s" format json.toString)
        }
      }
      Ok(retObj)
    } catch {
      case e:Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("Service is currently unavailable")
      }
    }
  }

  /**
   * Get billing info of a user in all game with range of date.
   * Json body contains: uid, and optional fields: from and to timestamp in Long format
   * If from, to timestamp fields is specified, it will find all bills from that time.
   * If not, all records of the user will be returned.
   * @return Ok status with list of billing records, otherwise Ok with empty json object, service unavailable
   */
  def getBillsByUidAllGames = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val from: Long = (json.getOrElse(null) \ "from").asOpt[Long].getOrElse(-1)
      val to: Long = (json.getOrElse(null) \ "to").asOpt[Long].getOrElse(-1)
      var result: List[BillingRecordModel] = null
      if (from < 0 || to < 0)
        result = billing.getAllBillsOfUserWithDate(uid, new Date(from), new Date(to))
      else
        result = billing.getAllBillsOfUser(uid)

      if (result != null) {
        Ok(Json.toJson(result))
      } else {
        Ok(Json.obj())
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("Service is currently unavailable")
      }
    }
  }

  /**
   * Get billing info of user in a game.
   * If from, to timestamp fields is specified, it will find all bills from that time.
   * If not, all records of the user will be returned.
   * @return Ok status with list of billing records, otherwise Ok with empty json object, service unavailable
   */
  def getBillsByUidOfGame() = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gameId: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(-1)
      val from: Long = (json.getOrElse(null) \ "from").asOpt[Long].getOrElse(-1)
      val to: Long = (json.getOrElse(null) \ "to").asOpt[Long].getOrElse(-1)

      var result: List[BillingRecordModel] = Nil

      if (from < 0 || to < 0) {
        result = billing.getBillsOfUserFromGame(uid, gameId)
      } else {
        result = billing.getBillsOfUserFromGameWithDate(uid, gameId, new Date(from), new Date(to))
      }
      if (result != null) {
        Ok(Json.toJson(result))
      } else {
        Ok(Json.obj())
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("Service is currently unavailable")
      }
    }
  }
}
