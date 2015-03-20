package spacerock.persistence.cassandra

import com.datastax.driver.core._
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

/**
 * Created by william on 2/24/15.
 */

trait CassandraLock {
//  def lock(key: String): Boolean
  def tryLock(key: String): Boolean
  def unlock(key: String): Boolean
  def lastError: Int
}

class CassandraLockDAO (implicit inj: Injector) extends CassandraLock with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                  = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Lock
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.lock SET do_lock = ? WHERE lock_name = ? if do_lock = ?;")
    if (ps != null)
      pStatements.put("Lock", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("INSERT INTO spacerock.lock (lock_name, do_lock) " +
      "VALUES (?, ?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("InsertLock", ps)
    else
      _lastError = sessionManager.lastError

    // unlock
    ps = sessionManager.prepare("UPDATE spacerock.lock SET do_lock = ? WHERE lock_name = ?;")
    if (ps != null)
      pStatements.put("Unlock", ps)
    else
      _lastError = sessionManager.lastError

  }

  override def tryLock(key: String): Boolean = {
    val ps: PreparedStatement = pStatements.get("Lock").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool(0, true)
    bs.setString("lock_name", key)
    bs.setBool(2, false)
    var result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      var row: Row = result.one()
      if (row != null) {
        if (!row.getBool(0)) {
          // if lock is not exists, try to insert new record
          val ps2: PreparedStatement = pStatements.get("InsertLock").getOrElse(null)
          if (ps2 == null || !sessionManager.connected) {
            Logger.error("Cannot connect to database")
            return false
          }
          val bs2: BoundStatement = new BoundStatement(ps2)
          bs2.setString("lock_name", key)
          bs2.setBool("do_lock", false)
          sessionManager.execute(bs2)
          // try to lock again
          result = sessionManager.execute(bs)
          row = result.one()
          if (row != null) {
            return row.getBool(0)
          }
        } else {
          return true
        }
      }
      false
    }
  }

  override def unlock(key: String): Boolean = {
    val ps: PreparedStatement = pStatements.get("Unlock").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("do_lock", false)
    bs.setString("lock_name", key)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        row.getBool(0)
      } else {
        false
      }
    }
  }
}
