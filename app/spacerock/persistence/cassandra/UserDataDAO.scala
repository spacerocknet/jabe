package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.SubscriberModel
import play.Logger
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by william on 1/13/15.
 */

trait UserData {
  def getInfoByUID(uuid: String): SubscriberModel
  def getInfoByUsername(userName: String): SubscriberModel
  def addBasicInfo(uid: String, userName: String, firstName: String, lastName: String,
                      email: String, fbId: String, locState: String, locRegion: String,
                      locCountry: String, appName: String): Boolean
  def addDeviceInfo(uid: String, platform: String, os: String, model: String,
                    phone: String, deviceUuid: String): Boolean
  def addDeviceInfo(subscriber: SubscriberModel): Boolean
  def updateLastSeenField(uuid: String): Boolean
  def getAllUsers(): List[SubscriberModel]
  def changeDevice(uid: String, platform: String, os: String, model: String, phone: String, deviceUuid: String): Boolean
  def close(): Unit
}

class UserDataDAO (implicit inj: Injector) extends UserData with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
        = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  /**
   * Update last seen field on users table
   * @param uid user id
   * @return true if success, otherwise false
   */
  override def updateLastSeenField(uid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateLastSeen", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setLong("last_seen", System.currentTimeMillis())
    session.execute(bs)
    true
  }

  /**
   * Add user's basic info.
   * @param uid user id
   * @param userName user name
   * @param firstName first name
   * @param lastName last name
   * @param email email
   * @param fbId user's facebook id
   * @param locState location state that user stays
   * @param locRegion location region
   * @param locCountry country
   * @param appName application name
   * @return true if success, otherwise false
   */
  override def addBasicInfo (uid: String, userName: String, firstName: String, lastName: String,
                                   email: String, fbId: String, locState: String, locRegion: String,
                                   locCountry: String, appName: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddUserInfo", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val time: Long = System.currentTimeMillis()
    bs.setString("uid", uid)
    bs.setString("user_name", userName)
    bs.setString("first_name", firstName)
    bs.setString("last_name", lastName)
    bs.setString("email", email)
    bs.setString("fb_id", fbId)
    bs.setString("state", locState)
    bs.setString("region", locRegion)
    bs.setString("country", locCountry)
    bs.setString("apps", appName)
    bs.setLong("last_seen", time)
    bs.setLong("registered_time", time)
    session.execute(bs)
    true
  }

  /**
   * Change device. This method will update current device's info and add uuid to set of devices.
   * Use Set of String, so there's no duplicated device in the list.
   * @param uid user id
   * @param platform new device's platform
   * @param os new device's operating system
   * @param model new device's model
   * @param phone user's phone number
   * @param deviceUuid new device's model
   * @return true if success, otherwise false
   */
  override def changeDevice(uid: String, platform: String, os: String, model: String,
                            phone: String, deviceUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("ChangeDevice", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val set: mutable.HashSet[String] = new mutable.HashSet[String]
    set.add(deviceUuid)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setString("device_uuid", deviceUuid)
    bs.setString("os", os)
    bs.setString("model", model)
    bs.setString("platform", platform)
    bs.setString("phone", phone)
    bs.setSet("device_list", set)
    session.execute(bs)
    true
  }

  /**
   * Same as method @changeDevice, this will add and set the submitted device's info to set and overwrite current info.
   * @param uid user id
   * @param platform device's platform
   * @param os device's operating system
   * @param model device's model
   * @param phone user's phone number
   * @param deviceUuid device's id
   * @return true if success, otherwise false
   */
  override def addDeviceInfo(uid: String, platform: String, os: String, model: String,
                             phone: String, deviceUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddDeviceInfo", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val set: mutable.HashSet[String] = new mutable.HashSet[String]
    set.add(deviceUuid)
    val bs: BoundStatement = new BoundStatement(ps)
    val time: Long = System.currentTimeMillis()
    bs.setString("uid", uid)
    bs.setString("device_uuid", deviceUuid)
    bs.setString("os", os)
    bs.setString("model", model)
    bs.setString("platform", platform)
    bs.setString("phone", phone)
    bs.setLong("registered_time", time)
    bs.setSet("device_list", set)
    session.execute(bs)
    true
  }

  /**
   * See method @addDeviceInfo. This method do the same thing except it deals with the input as subscriber model
   * @param subscriber subscriber model
   * @return true if success, otherwise false
   */
  override def addDeviceInfo(subscriber: SubscriberModel): Boolean = {
    addDeviceInfo(subscriber.uid, subscriber.platform, subscriber.os, subscriber.model,
                  subscriber.phone, subscriber.deviceUuid)
  }

  /**
   * Get user's information by user id
   * @param uid user id
   * @return Subscriber model if success, otherwise null
   */
  override def getInfoByUID(uid: String): SubscriberModel = {
    val ps: PreparedStatement = pStatements.get("GetUserInfoByUID").orNull(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val subscriber: SubscriberModel = new SubscriberModel(row.getString("uid"), row.getString("platform"),
        row.getString("os"), row.getString("model"),
        row.getString("phone"),row.getString("device_uuid"), row.getString("email"),
        row.getString("fb_id"), row.getString("state"),
        row.getString("region"), row.getString("country"), row.getString("apps"))
      subscriber.deviceSet = row.getSet("device_list", classOf[String]).toSet
      subscriber
    } else {
      null
    }
  }

  /**
   * Get user's information by username
   * @param userName
   * @return Subscriber model if success, otherwise null
   */
  def getInfoByUsername(userName: String): SubscriberModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetUserInfoByUsername", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("user_name", userName)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val subscriber: SubscriberModel = new SubscriberModel(row.getString("uid"), row.getString("platform"),
        row.getString("os"), row.getString("model"),
        row.getString("phone"), row.getString("device_uuid"), row.getString("email"),
        row.getString("fb_id"), row.getString("state"),
        row.getString("region"), row.getString("country"), row.getString("apps"))
      subscriber.deviceSet = row.getSet("device_list", classOf[String]).toSet
      subscriber
    } else {
      null
    }
  }

  /**
   * Get all user from system.
   * @return list of subscriber models
   */
  override def getAllUsers(): List[SubscriberModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllUsers", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[SubscriberModel] = scala.collection.mutable.ListBuffer()
    for (row: Row <- result.all()) {
      if (row != null) {
        val sm: SubscriberModel = new SubscriberModel(row.getString("uid"), row.getString("platform"),
          row.getString("os"), row.getString("model"),
          row.getString("phone"), row.getString("device_uuid"), row.getString("email"),
          row.getString("fb_id"), row.getString("state"),
          row.getString("region"), row.getString("country"), row.getString("apps"))
        sm.deviceSet = row.getSet("device_list", classOf[String]).toSet
        l.add(sm)
      }
    }
    l.toList
  }

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
    // update last seen
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.users SET last_seen = ? where uid = ?;")
    pStatements.put("UpdateLastSeen", ps)

    // change user device
    ps = session.prepare("UPDATE spacerock.users SET device_list = device_list + ?, device_uuid = ?, platform = ?, os = ?, " +
      "model = ?, phone = ? WHERE uid = ?;")
    pStatements.put("ChangeDevice", ps)

    // Add user information
    ps = session.prepare("UPDATE spacerock.users SET user_name = ?, first_name = ?, last_name = ?, " +
      "email = ?, fb_id = ?, state = ?, " +
      "region = ?, country = ?, apps = ?, registered_time = ?, last_seen = ? WHERE uid = ?;")
    pStatements.put("AddUserInfo", ps)

    // add device information
    ps = session.prepare("INSERT INTO spacerock.users (uid, device_uuid, platform, os, " +
      "model, phone, registered_time, device_list) " +
      "VALUES " +
      "(?, ?, ?, ?, ?, ?, ?, ?)")
    pStatements.put("AddDeviceInfo", ps)

    // Get user info
    ps = session.prepare("SELECT * from spacerock.users where uid = ?;")
    pStatements.put("GetUserInfoByUID", ps)
    ps = session.prepare("SELECT * from spacerock.users where user_name = ? ALLOW FILTERING;")
    pStatements.put("GetUserInfoByUsername", ps)

    // Get all users
    ps = session.prepare("SELECT * FROM spacerock.users ALLOW FILTERING;")
    pStatements.put("GetAllUsers", ps)
  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }
}

