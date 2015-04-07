package spacerock.persistence.cassandra

import java.net.InetAddress

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet}
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import scala.collection.JavaConversions._

/**
 * Created by william on 3/20/15.
 */
trait ServerInfo {
  def insertOrUpdateServerInfo(ip: String, seq: Int): Boolean
  def getSeqInfo(ip: String): Long
  def getSeqInfo(ip: InetAddress): Long
  def insertOrUpdateServerInfo(ip: InetAddress, seq: Int): Boolean
  def lastError: Int
}

class ServerInfoDAO (implicit inj: Injector) extends  ServerInfo with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                   = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // call initialization method to prepare statements
  init()

  /**
   * Initialize statements of server info table
   * @return
   */
  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new server info
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.serverinfo SET seq = seq + ? " +
      "WHERE server_ip = ?;")
    if (ps != null)
      pStatements.put("AddUpdateInfo", ps)
    else
      _lastError = sessionManager.lastError

    // Get server's seq number
    ps = sessionManager.prepare("SELECT seq FROM spacerock.serverinfo WHERE server_ip = ?;")
    if (ps != null)
      pStatements.put("GetSeqNum", ps)
    else
      _lastError = sessionManager.lastError
  }

  /**
   * Insert or update server info with ip and seq number.
   * @param ip IP of Jabe server
   * @param seq seq number that needs to insert or update
   * @return true if success, otherwise false
   * @deprecated use @insertOrUpdateServerInfo(InetAddress, Int) instead
   */
  override def insertOrUpdateServerInfo(ip: String, seq: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddUpdateInfo", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong(0, seq)
    bs.setString(1, ip)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Insert or update server info with ip and seq number.
   * @param ip IP of Jabe server
   * @param seq seq number that needs to insert or update
   * @return true if success, otherwise false
   */
  override def insertOrUpdateServerInfo(ip: InetAddress, seq: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddUpdateInfo", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong(0, seq)
    bs.setInet(1, ip)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Get seq number of Jabe server.
   * @param ip IP of Jabe server
   * @return >= 0 if success, otherwise -1. See _lastError to investigate to error.
   * @deprecated use getSeqInfo(InetAddress) instead
   */
  override def getSeqInfo(ip: String): Long = {
    val ps: PreparedStatement = pStatements.getOrElse("GetSeqNum", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return -1
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, ip)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (r <- result.all()) {
        if (r != null) {
          return r.getLong("seq")
        }
      }
      -1
    } else {
      _lastError = sessionManager.lastError
      -1
    }
  }

  /**
   * Get seq number of Jabe server.
   * @param ip IP of Jabe server
   * @return >= 0 if success, otherwise -1. See _lastError to investigate to error.
   */
  override def getSeqInfo(ip: InetAddress): Long = {
    val ps: PreparedStatement = pStatements.getOrElse("GetSeqNum", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return -1
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInet(0, ip)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return row.getLong("seq")
        }
      }
      -1
    } else {
      _lastError = sessionManager.lastError
      -1
    }
  }
}
