package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.TokenInfo
import play.Logger
import scaldi.{Injector, Injectable}
import scala.collection.JavaConversions._
import java.util.Date

/**
 * Created by william on 3/3/15.
 */
trait AuthCode{
  def addNewCode(code: String, createdTime: Date, expiredTime: Date, status: Boolean, ttl: Long): Boolean
  def getAuthCode(code: String): TokenInfo
  def updateStatus(code: String, status: Boolean): Boolean
  def updateCreatedTime(code: String, createdTime: Date): Boolean
  def updateExpiredTime(code: String, expiredTime: Date): Boolean
  def close(): Unit
}

class AuthCodeDAO(implicit inj: Injector) extends AuthCode with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  def connect(node: String): Boolean = {
    cluster = Cluster.builder().addContactPoint(node).build()
    val metadata = cluster.getMetadata()
    var countHost: Int = 0
    metadata.getAllHosts() map {
      case host => countHost += 1
    }
    session = cluster.connect()

    if (countHost < 1)
      false
    else {
      init()
      true
    }
  }

  def init() = {
    // Insert new auth code
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.authcode " +
                                "(code, created_time, expired_time, status) " +
                                "VALUES (?, ?, ?, ?) IF NOT EXISTS;")
    pStatements.put("AddNewCode", ps)

    // Get auth code info
    ps = session.prepare("SELECT created_time, expired_time, status FROM spacerock.authcode WHERE code = ?;")
    pStatements.put("GetAuthCode", ps)

    // Update status
    ps = session.prepare("INSERT INTO spacerock.authcode (code, status) VALUES (?, ?);")
    pStatements.put("UpdateStatus", ps)

    // Update created time
    ps = session.prepare("INSERT INTO spacerock.authcode (code, created_time) VALUES (?, ?);")
    pStatements.put("UpdateCreatedTime", ps)

    // Update status
    ps = session.prepare("INSERT INTO spacerock.authcode (code, expired_time) VALUES (?, ?);")
    pStatements.put("UpdateExpiredTime", ps)

  }

  override def close(): Unit = {
    if (cluster != null)
      cluster.close()
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
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("created_time", createdTime)
    bs.setDate("expired_time", expiredTime)
    bs.setBool("status", status)

    session.execute(bs)

    true
  }

  /**
   * Check authentication code
   * @param code code to check
   * @return true if valid, otherwise false
   */
  override def getAuthCode(code: String): TokenInfo = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAuthCode", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    val res: ResultSet = session.execute(bs)
    val row: Row = res.one()
    if (row != null) {
      return new TokenInfo(code, row.getDate("created_time"), row.getDate("expired_time"), row.getBool("status"))
    }
    Logger.warn("Cannot find token in db")
    null
  }

  /**
   * Update authentication code's status.
   * @param code auth code
   * @param status status to update
   * @return true if exists and update successfully, otherwise false
   */
  override def updateStatus(code: String, status: Boolean): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateStatus", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setBool("status", status)
    val r: ResultSet = session.execute(bs)
    val row: Row = r.one()
    if (row != null && row.getBool(0))
      return true
    else
      return false
  }

  /**
   * Update token's created time
   * @param code auth code
   * @param createdTime token's created time
   * @return true if exists and update successfully, otherwise false
   */
  override def updateCreatedTime(code: String, createdTime: Date): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateCreatedTime", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("created_time", createdTime)
    val r: ResultSet = session.execute(bs)
    val row: Row = r.one()
    if (row != null && row.getBool(0))
      return true
    else
      return false
  }

  /**
   * Update token's expired time
   * @param code auth code
   * @param expiredTime token's expired time
   * @return true if exists and update successfully, otherwise false
   */
  override def updateExpiredTime(code: String, expiredTime: Date): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateExpiredTime", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("code", code)
    bs.setDate("expired_time", expiredTime)
    val r: ResultSet = session.execute(bs)
    val row: Row = r.one()
    if (row != null && row.getBool(0))
      return true
    else
      return false
  }
}
