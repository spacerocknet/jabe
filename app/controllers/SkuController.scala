package controllers

import java.util.Date

import models.SkuModel
import play.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants
import spacerock.persistence.cassandra._

/**
 * Created by william on 2/23/15.
 */
class SkuController(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [UserData]
  val idGenerator = inject [IdGenerator]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")
  val uidBlock = inject [UidBlock]
  val idLocker = inject [CassandraLock]
  val sku = inject [Sku]

  /**
   * Add new Sku info. Json body contains; description, unit-price, start-time, expired-time, extra-date, description.
   * Also the method will generate sku's id based on key of redis, using IdGenerator.
   * @return sku-id if success, otherwise failed status, service unavailable otherwise.
   */
  def addNewSku() = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      // get id
      val skuId: Int = idGenerator.generateNextId(Constants.REDIS_SKY_ID_KEY).toInt
      val description: String = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")
      val unitPrice: Float = (json.getOrElse(null) \ "unit-price").asOpt[Float].getOrElse(0.0f)
      val startTime: Long = (json.getOrElse(null) \ "start-time").asOpt[Long].getOrElse(-1)
      val expiredTime: Long = (json.getOrElse(null) \ "expired-time").asOpt[Long].getOrElse(-1)
      val extraData: String = (json.getOrElse(null) \ "extra-data").asOpt[String].getOrElse("")
      val discount: Float = (json.getOrElse(null) \ "description").asOpt[Float].getOrElse(0.0f)
      if (sku.addNewSku(skuId, description, unitPrice, new Date(startTime), new Date(expiredTime),
                    extraData, discount)) {
        Ok("""{"sku-id" : %d}""" format skuId)
      } else {
        Ok(FailedStatus)
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        ServiceUnavailable("System is currently unavailable")
      }
    }
  }

  /**
   * Get sku info by sku id
   * @param skuId sku id
   * @return sku info if success, otherwise failed status
   */
  def getSkuInfo(skuId: Int) = Action {
    val res: SkuModel = sku.getSkuInfo(skuId)
    if (res != null) {
      Ok(sku.toString)
    } else {
      Ok(FailedStatus)
    }
  }
}
