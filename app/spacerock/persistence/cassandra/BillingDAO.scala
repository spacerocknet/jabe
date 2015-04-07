package spacerock.persistence.cassandra

import java.util.Date

import com.datastax.driver.core._
import models.BillingRecordModel
import play.Logger
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 2/23/15.
 */

trait Billing {
  def addNewBill(uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, totalDiscount: Float): Boolean
  def addNewBill(br: BillingRecordModel): Boolean
  def getAllBillsOfUser(uid: String): List[BillingRecordModel]
  def getAllBillsOfUserWithDate(uid: String, from: Date, to: Date): List[BillingRecordModel]
  def getBillsOfUserFromGame(uid: String, gameId: Int): List[BillingRecordModel]
  def getBillsOfUserFromGameWithDate(uid: String, gameId: Int, from: Date, to: Date): List[BillingRecordModel]
  def lastError: Int
}

class BillingDAO (implicit inj: Injector) extends Billing with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
            = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError
  // initialize prepared statements
  init

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new bill
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.billing (uid, ts, game_id, sku_id, " +
      "n_items, discount) " +
      "VALUES (?, ?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddNewBill", ps)
    else
      _lastError = sessionManager.lastError

    // Get all billing record of a user
    ps = sessionManager.prepare("SELECT * FROM spacerock.billing WHERE uid = ?;")
    if (ps != null)
      pStatements.put("GetBills", ps)
    else
      _lastError = sessionManager.lastError

    // Get all billing record of a user in a rage of date
    ps = sessionManager.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND ts > ? and ts < ?;")
    if (ps != null)
      pStatements.put("GetBillsWithBoundDate", ps)
    else
    _lastError = sessionManager.lastError

    // get all billing records of a user in a game
    ps = sessionManager.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND game_id = ?;")
    if (ps != null)
      pStatements.put("GetBillsFromGame", ps)
    else
      _lastError = sessionManager.lastError

    // get all billing records of a user in a game
    ps = sessionManager.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND ts < ? AND ts > ? AND game_id = ?;")
    if (ps != null)
      pStatements.put("GetBillsFromGameWithDate", ps)
    else
      _lastError = sessionManager.lastError
  }

  override def addNewBill(uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, discount: Float): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewBill", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setDate("ts", ts)
    bs.setInt("game_id", gameId)
    bs.setInt("sku_id", skuId)
    bs.setInt("n_items", nItems)
    bs.setFloat("discount", discount)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def addNewBill(br: BillingRecordModel): Boolean = {
    addNewBill(br.uid, br.ts, br.gameId, br.skuId, br.nItems, br.discount)
  }

  override def getAllBillsOfUser(uid: String): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBills", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
      for (r: Row <- result.all()) {
        if (r != null) {
          val sku: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
            r.getDate("ts"),
            r.getInt("game_id"),
            r.getInt("sku_id"),
            r.getInt("n_items"),
            r.getFloat("discount"))
          l.add(sku)
        }
      }
      l.toList
    }
  }

  override def getAllBillsOfUserWithDate(uid: String, from: Date, to: Date): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsWithBoundDate", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setDate(1, from)
    bs.setDate(2, to)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
      for (r: Row <- result.all()) {
        if (r != null) {
          val sku: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
            r.getDate("ts"),
            r.getInt("game_id"),
            r.getInt("sku_id"),
            r.getInt("n_items"),
            r.getFloat("discount"))
          l.add(sku)
        }
      }
      l.toList
    }
  }

  override def getBillsOfUserFromGame(uid: String, gameId: Int): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsFromGame", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gameId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val r: Row = result.one()
      val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
      if (r != null) {
        val br: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
          r.getDate("ts"),
          r.getInt("game_id"),
          r.getInt("sku_id"),
          r.getInt("n_items"),
          r.getFloat("discount"))
        l.add(br)
      }
      l.toList
    }
  }

  override def getBillsOfUserFromGameWithDate(uid: String, gameId: Int,
                                              from: Date, to: Date): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsFromGameWithDate", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gameId)
    bs.setDate(2, from)
    bs.setDate(3, to)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val r: Row = result.one()
      val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
      if (r != null) {
        val br: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
          r.getDate("ts"),
          r.getInt("game_id"),
          r.getInt("sku_id"),
          r.getInt("n_items"),
          r.getFloat("discount"))
        l.add(br)
      }
      l.toList
    }
  }

}
