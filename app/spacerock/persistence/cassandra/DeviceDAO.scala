package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.{DeviceModel}
import play.Logger
import scaldi.{Injector, Injectable}
import java.util.Date
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 2/23/15.
 */

trait Device {
  def addNewDevice(dUuid: String, rt: Date, uid: String = "", os: String = "", platForm: String = "", model: String = "",
                    phone: String = ""): Boolean
  def addNewDevice(device: DeviceModel): Boolean
  def getInfoByDuuid(dUuid: String): DeviceModel
  def getInfoByPhone(phone: String): List[DeviceModel]
  def getInfoByUid(uid: String): List[DeviceModel]
  def lastError: Int
}

class DeviceDAO (implicit inj: Injector) extends Device with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                 = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new bill
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.device (" +
      "duuid, uid, os, platform, model, phone, registered_time) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddNewDevice", ps)
    else
      _lastError = sessionManager.lastError

    // Get device's info
    ps = sessionManager.prepare("SELECT * FROM spacerock.device WHERE duuid = ?;")
    if (ps != null)
      pStatements.put("GetInfoByDUuid", ps)
    else
      _lastError = sessionManager.lastError

    // get devices by phone
    ps = sessionManager.prepare("SELECT * FROM spacerock.device WHERE phone = ?;")
    if (ps != null)
      pStatements.put("GetInfoByPhone", ps)
    else
      _lastError = sessionManager.lastError

    // get devices by phone
    ps = sessionManager.prepare("SELECT * FROM spacerock.device WHERE uid = ?;")
    if (ps != null)
      pStatements.put("GetInfoByUid", ps)
    else
      _lastError = sessionManager.lastError
  }


  /**
   * Add new device info to system.
   * @param dUuid device uuid
   * @param rt register time
   * @param uid user id
   * @param os device's operating system
   * @param platform platform that device based on
   * @param model device's model
   * @param phone user phone number
   * @return true if success, otherwise false
   */
  override def addNewDevice(dUuid: String, rt: Date, uid: String, os: String, platform: String,
                            model: String, phone: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewDevice", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("duuid", dUuid)
    bs.setString("uid", uid)
    bs.setString("os", os)
    bs.setString("platform", platform)
    bs.setString("model", model)
    bs.setString("phone", phone)
    bs.setDate("registered_time", rt)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Get information of all devices by phone
   * @param phone user phone
   * @return list of devices
   */
  override def getInfoByPhone(phone: String): List[DeviceModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByPhone", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("phone", phone)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[DeviceModel] = new ListBuffer[DeviceModel]
      for (r: Row <- result.all()) {
        if (r != null) {
          val dm: DeviceModel = new DeviceModel(r.getString("duuid"),
            r.getDate("registered_time"),
            r.getString("uid"),
            r.getString("os"),
            r.getString("platform"),
            r.getString("model"),
            r.getString("phone"))
          l.add(dm)
        }
      }
      l.toList
    }
  }

  /**
   * Get device's information by device uuid
   * @param dUuid device uuid
   * @return device's info if existed, or null if not
   */
  override def getInfoByDuuid(dUuid: String): DeviceModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByDUuid", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("duuid", dUuid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (r: Row <- result.all()) {
        if (r != null) {
          val dm: DeviceModel = new DeviceModel(r.getString("duuid"),
            r.getDate("registered_time"),
            r.getString("uid"),
            r.getString("os"),
            r.getString("platform"),
            r.getString("model"),
            r.getString("phone"))
          return dm
        }
      }
      null
    }
  }

  /**
   * Add new device by device model
   * @param d device model
   * @return true if success, otherwise false
   */
  override def addNewDevice(d: DeviceModel): Boolean = {
    addNewDevice(d.dUuid, d.rt, d.uid, d.os, d.platform, d.model, d.phone)
  }

  /**
   * Get all device's info by uid.
   * @param uid user id
   * @return list of device
   */
  override def getInfoByUid(uid: String): List[DeviceModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByUid", null)
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
      val l: ListBuffer[DeviceModel] = new ListBuffer[DeviceModel]
      for (r: Row <- result.all()) {
        if (r != null) {
          val dm: DeviceModel = new DeviceModel(r.getString("duuid"),
            r.getDate("registered_time"),
            r.getString("uid"),
            r.getString("os"),
            r.getString("platform"),
            r.getString("model"),
            r.getString("phone"))
          l.add(dm)
        }
      }
      l.toList
    }
  }
}
