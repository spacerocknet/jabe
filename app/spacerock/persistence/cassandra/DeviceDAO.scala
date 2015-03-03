package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.{DeviceModel}
import play.Logger
import scaldi.{Injector, Injectable}
import java.util.Date
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
  def close(): Unit
}

class DeviceDAO (implicit inj: Injector) extends Device with Injectable {
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
    // Insert new bill
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.device (" +
      "duuid, uid, os, platform, model, phone, registered_time) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?);")
    pStatements.put("AddNewDevice", ps)

    // Get device's info
    ps = session.prepare("SELECT * FROM spacerock.device WHERE duuid = ?;")
    pStatements.put("GetInfoByDUuid", ps)

    // get devices by phone
    ps = session.prepare("SELECT * FROM spacerock.device WHERE phone = ?;")
    pStatements.put("GetInfoByPhone", ps)

    // get devices by phone
    ps = session.prepare("SELECT * FROM spacerock.device WHERE uid = ?;")
    pStatements.put("GetInfoByUid", ps)
  }

  override def close() = {
    if (cluster != null)
      cluster.close()
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
    if (ps == null || !isConnected) {
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

    session.execute(bs)

    true
  }

  /**
   * Get information of all devices by phone
   * @param phone user phone
   * @return list of devices
   */
  override def getInfoByPhone(phone: String): List[DeviceModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByPhone", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("phone", phone)
    val result: ResultSet = session.execute(bs)
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

  /**
   * Get device's information by device uuid
   * @param dUuid device uuid
   * @return device's info if existed, or null if not
   */
  override def getInfoByDuuid(dUuid: String): DeviceModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetInfoByDUuid", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("duuid", dUuid)
    val result: ResultSet = session.execute(bs)
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
        return dm
      }
    }
    null
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
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    val result: ResultSet = session.execute(bs)
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
