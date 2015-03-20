package controllers

import java.util.Date

import models.SkuModel
import play.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, Controller}
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants
import spacerock.persistence.cassandra._
import spacerock.utils.{StaticVariables, IdGenerator}

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
  implicit val skuFmt = Json.format[SkuModel]

  /**
   * Add new Sku info. Json body contains; description, unit-price, start-time, expired-time, extra-date, description.
   * Also the method will generate sku's id based on key of redis, using IdGenerator.
   * @return sku-id if success, otherwise failed status, service unavailable otherwise.
   */
  def addNewSku = Action { request =>
    var retObj: JsObject = FailedStatus
    try {
      val json: Option[JsValue] = request.body.asJson
      // get id
      val skuId: Int = idGenerator.generateNextId(Constants.REDIS_SKU_ID_KEY).toInt
      val description: String = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")
      val unitPrice: Float = (json.getOrElse(null) \ "unit_price").asOpt[Float].getOrElse(0.0f)
      val startTime: Long = (json.getOrElse(null) \ "start_time").asOpt[Long].getOrElse(-1)
      val expiredTime: Long = (json.getOrElse(null) \ "expired_time").asOpt[Long].getOrElse(-1)
      val extraData: String = (json.getOrElse(null) \ "extra_data").asOpt[String].getOrElse("")
      val discount: Float = (json.getOrElse(null) \ "discount").asOpt[Float].getOrElse(0.0f)

      if (sku.addNewSku(skuId, description, unitPrice, new Date(startTime), new Date(expiredTime),
                    extraData, discount)) {
        retObj = Json.obj("sku_id" -> skuId)
      } else {
        Logger.warn("Cannot add new sku. Please check database again")
        retObj = Json.obj("status" -> sku.lastError)
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
   * Get sku info by sku id
   * @param skuId sku id
   * @return sku info if success, otherwise failed status
   */
  def getSkuInfo(skuId: Int) = Action {
    val res: SkuModel = sku.getSkuInfo(skuId)
    if (res != null) {
      Ok(Json.toJson(res))
    } else {
      Ok(Json.obj("status" -> sku.lastError))
    }
  }
}
