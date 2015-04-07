package spacerock.persistence.cassandra

import java.util.Date

import com.datastax.driver.core._
import models.SkuModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

/**
 * Created by william on 2/24/15.
 */
trait Sku {
  def addNewSku(skuId: Int, description: String, unitPrice: Float, startDate: Date,
                expiredTime: Date, extraData: String, discount: Float): Boolean
  def addNewSku(sku: SkuModel): Boolean
  def getSkuInfo(skuId: Int): SkuModel
  def lastError: Int
}

class SkuDAO (implicit inj: Injector) extends Sku with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new sku record
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.sku (sku_id, description, unit_price," +
      "start_time, expired_time, extra_data, discount) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddNewSku", ps)
    else
      _lastError = sessionManager.lastError


    // get sku info
    ps = sessionManager.prepare("SELECT * FROM spacerock.sku WHERE sku_id = ?;")
    if (ps != null)
      pStatements.put("GetSkuInfo", ps)
    else
      _lastError = sessionManager.lastError

  }

/**
 * Add new Sku
 * @param skuId sku id
 * @param description sku description
 * @param unitPrice sku's unit price
 * @param startTime start time of sku
 * @param expiredTime expired time of sku
 * @param extraData extra data
 * @param discount discount information
 * @return true if success, otherwise false
 */
  override def addNewSku(skuId: Int, description: String, unitPrice: Float, startTime: Date,
                          expiredTime: Date, extraData: String, discount: Float): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewSku", null)
    if (ps == null || !sessionManager.connected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("sku_id", skuId)
    bs.setString("description", description)
    bs.setFloat("unit_price", unitPrice)
    bs.setDate("start_time", startTime)
    bs.setDate("expired_time", expiredTime)
    bs.setString("extra_data", extraData)
    bs.setFloat("discount", discount)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

/**
 * Add new sku with sku model
 * @param sku sku model
 * @return true if success, otherwise false
 */
  override def addNewSku(sku: SkuModel): Boolean = {
    addNewSku(sku.skuId, sku.description, sku.unitPrice, sku.startTime, sku.expiredTime,
      sku.extraData, sku.discount)
  }

/**
 * Get sku info by sku id
 * @param skuId sku id
 * @return sku model if success, otherwise false
 */
  override def getSkuInfo(skuId: Int): SkuModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetSkuInfo", null)
    if (ps == null || !sessionManager.connected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("sku_id", skuId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val r: Row = result.one()
      if (r != null) {
        val sku: SkuModel = new SkuModel(r.getInt("sku_id"),
          r.getString("description"),
          r.getFloat("unit_price"),
          r.getDate("start_time"),
          r.getDate("expired_time"),
          r.getString("extra_data"),
          r.getFloat("discount"))
        sku
      } else {
        null
      }
    }
  }
}
