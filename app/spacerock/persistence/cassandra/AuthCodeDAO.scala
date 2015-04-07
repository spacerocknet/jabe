package spacerock.persistence.cassandra

import java.util.Date

import com.datastax.driver.core._
import models.TokenInfo
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

/**
 * Created by william on 3/3/15.
 */
trait AuthCode{
  def addNewCode(code: String, createdTime: Date, expiredTime: Date, status: Boolean, ttl: Long): Boolean
  def getAuthCode(code: String): TokenInfo
  def updateStatus(code: String, status: Boolean): Boolean
  def updateCreatedTime(code: String, createdTime: Date): Boolean
  def updateExpiredTime(code: String, expiredTime: Date): Boolean
  def lastError: Int
}

class AuthCodeDAO(implicit inj: Injector) extends AuthCode with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                  = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new auth code
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.authcode " +
                                "(code, created_time, expired_time, status) " +
                                "VALUES (?, ?, ?, ?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("AddNewCode", ps)
    else
      _lastError = sessionManager.lastError

    // Get auth code info
    ps = sessionManager.prepare("SELECT created_time, expired_time, status FROM spacerock.authcode WHERE code = ?;")
    if (ps != null)
      pStatements.put("GetAuthCode", ps)
    else
      _lastError = sessionManager.lastError

    // Update status
    ps = sessionManager.prepare("INSERT INTO spacerock.authcode (code, status) VALUES (?, ?);")
    if (ps != null)
      pStatements.put("UpdateStatus", ps)
    else
      _lastError = sessionManager.lastError

    // Update created time
    ps = sessionManager.prepare("INSERT INTO spacerock.authcode (code, created_time) VALUES (?, ?);")
    if (ps != null)
      pStatements.put("UpdateCreatedTime", ps)
    else
      _lastError = sessionManager.lastError

    // Update status
    ps = sessionManager.prepare("INSERT INTO spacerock.authcode (code, expired_time) VALUES (?, ?);")

    if (ps != null)
      pStatements.put("UpdateExpiredTime", ps)
    else
      _lastError = sessionManager.lastError
  }

  /**
   * Add new auth code to database
   * @param code authentication code
   * @param createdTime token's created time
   * @param expiredTime token's expired time
   * @param status token's status
   * @param ttl time to live
   * @return true if success, otherwise false
   */
  override def addNewCode(code: String, createdTime: Date, expiredTime: Date, status: Boolean, ttl: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewCode", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("created_time", createdTime)
    bs.setDate("expired_time", expiredTime)
    bs.setBool("status", status)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
     false
    }
  }

  /**
   * Check authentication code
   * @param code code to check
   * @return true if valid, otherwise false
   */
  override def getAuthCode(code: String): TokenInfo = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAuthCode", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        return new TokenInfo(code, row.getDate("created_time"), row.getDate("expired_time"), row.getBool("status"))
      }
      Logger.warn("Cannot find token in db")
      null
    }
  }

  /**
   * Update authentication code's status.
   * @param code auth code
   * @param status status to update
   * @return true if exists and update successfully, otherwise false
   */
  override def updateStatus(code: String, status: Boolean): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateStatus", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setBool("status", status)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        row.getBool(0)
      } else {
        _lastError = sessionManager.lastError
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Update token's created time
   * @param code auth code
   * @param createdTime token's created time
   * @return true if exists and update successfully, otherwise false
   */
  override def updateCreatedTime(code: String, createdTime: Date): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateCreatedTime", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("created_time", createdTime)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        row.getBool(0)
      } else {
        _lastError = sessionManager.lastError
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Update token's expired time
   * @param code auth code
   * @param expiredTime token's expired time
   * @return true if exists and update successfully, otherwise false
   */
  override def updateExpiredTime(code: String, expiredTime: Date): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateExpiredTime", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("expired_time", expiredTime)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        row.getBool(0)
      } else {
        _lastError = sessionManager.lastError
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }
}
