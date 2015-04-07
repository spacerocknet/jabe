package spacerock.persistence.cassandra

import java.util

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

// TODO change phone number
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

  // initialize prepared statements
  init

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

    ps = sessionManager.prepare("UPDATE spacerock.device_phone SET duuids = duuids + ? " +
      "WHERE phone = ?;")
    if (ps != null)
      pStatements.put("UpdateDUuidPhoneI", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.device_uid SET duuids = duuids + ? " +
      "WHERE uid = ?;")
    if (ps != null)
      pStatements.put("UpdateDUuidUidI", ps)
    else
      _lastError = sessionManager.lastError

    // Get device's info
    ps = sessionManager.prepare("SELECT * FROM spacerock.device WHERE duuid = ?;")
    if (ps != null)
      pStatements.put("GetInfoByDUuid", ps)
    else
      _lastError = sessionManager.lastError

    // get devices by phone
    ps = sessionManager.prepare("SELECT duuids FROM spacerock.device_phone WHERE phone = ?;")
    if (ps != null)
      pStatements.put("GetInfoByPhoneI", ps)
    else
      _lastError = sessionManager.lastError

    // get devices by phone
    ps = sessionManager.prepare("SELECT duuids FROM spacerock.device_uid WHERE uid = ?;")
    if (ps != null)
      pStatements.put("GetInfoByUidI", ps)
    else
      _lastError = sessionManager.lastError
  }

  private def updateDeviceUid(uid: String, dUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateDUuidUidI", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setSet("duuids", Set[String]{dUuid})
    bs.setString("uid", uid)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  private def updateDevicePhone(phone: String, dUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateDUuidPhoneI", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setSet("duuids", Set[String]{dUuid})
    bs.setString("phone", phone)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
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
      if (updateDevicePhone(phone, dUuid) && updateDeviceUid(uid, dUuid)) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        false
      }
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
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByPhoneI", null)
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
        val duuids: util.Set[String] = r.getSet("duuids", classOf[String])
          if (duuids != null) {
            for (duuid <- duuids) {
              val dm: DeviceModel = getInfoByDuuid(duuid)
              if (dm != null)
                l.add(dm)
            }
          }
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
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByUidI", null)
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
          val duuids: util.Set[String] = r.getSet("duuids", classOf[String])
          if (duuids != null) {
            for (duuid <- duuids) {
              val dm: DeviceModel = getInfoByDuuid(duuid)
              if (dm != null)
                l.add(dm)
            }
          }
        }
      }
      l.toList
    }
  }
}
