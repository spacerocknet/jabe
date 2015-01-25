package spacerock.persistence

import java.util.UUID

import com.datastax.driver.core._
import models.Subscriber
import play.Logger
import scala.collection.JavaConversions._
import scaldi.{Injectable, Injector}

/**
 * Created by william on 1/13/15.
 */

trait NewUserData {
  def getUserInfoByUID(uuid: String): Subscriber
  def getUserInfoByUsername(userName: String): Subscriber
  def addUserBasicInfo(uid: String, userName: String, firstName: String, lastName: String,
                      email: String, fbId: String, locState: String, locRegion: String,
                      locCountry: String, appName: String): Boolean
  def addDeviceInfo(uid: String, platform: String, os: String, model: String, phone: String, deviceUuid: String): Boolean
  def addDeviceInfo(subscriber: Subscriber): Boolean
  def updateLastSeenField(uuid: String): Boolean
  def getAllUsers(): List[Subscriber]
  def changeDevice(uid: String, platform: String, os: String, model: String, phone: String, deviceUuid: String): Boolean
}

class NewUserDataDAO (implicit inj: Injector) extends NewUserData with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
        = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  override def updateLastSeenField(uuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateLastSeen", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uuid)
    bs.setLong("last_seen", System.currentTimeMillis())
    session.execute(bs)
    true
  }

  override def addUserBasicInfo (uid: String, userName: String, firstName: String, lastName: String,
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

  override def changeDevice(uid: String, platform: String, os: String, model: String,
                            phone: String, deviceUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("ChangeDevice", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setString("device_uuid", deviceUuid)
    bs.setString("os", os)
    bs.setString("model", model)
    bs.setString("platform", platform)
    bs.setString("phone", phone)
    session.execute(bs)
    true
  }

  override def addDeviceInfo(uid: String, platform: String, os: String, model: String,
                             phone: String, deviceUuid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddDeviceInfo", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val time: Long = System.currentTimeMillis()
    bs.setString("uid", uid)
    bs.setString("device_uuid", deviceUuid)
    bs.setString("os", os)
    bs.setString("model", model)
    bs.setString("platform", platform)
    bs.setString("phone", phone)
    bs.setLong("registered_time", time)
    session.execute(bs)
    true
  }

  override def addDeviceInfo(subscriber: Subscriber): Boolean = {
    addDeviceInfo(subscriber.uid, subscriber.platform, subscriber.os, subscriber.model,
                  subscriber.phone, subscriber.deviceUuid)
  }

  override def getUserInfoByUID(uid: String): Subscriber = {
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
      val subscriber: Subscriber = new Subscriber(row.getString("uid"), row.getString("platform"),
        row.getString("os"), row.getString("model"),
        row.getString("phone"),row.getString("device_uuid"), row.getString("email"),
        row.getString("fb_id"), row.getString("state"),
        row.getString("region"), row.getString("country"), row.getString("apps"))
      subscriber
    } else {
      null
    }
  }

  def getUserInfoByUsername(userName: String): Subscriber = {
    val ps: PreparedStatement = pStatements.getOrElse("GetUserInfoByUsername", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("user_name", userName)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    val subscriber: Subscriber = new Subscriber(row.getString("uid"), row.getString("platform"),
      row.getString("os"), row.getString("model"),
      row.getString("phone"),row.getString("device_uuid"), row.getString("email"),
      row.getString("fb_id"), row.getString("state"),
      row.getString("region"), row.getString("country"), row.getString("apps"))
    subscriber
  }

  override def getAllUsers(): List[Subscriber] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllUsers", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[Subscriber] = scala.collection.mutable.ListBuffer()
    for (row: Row <- result.all()) {
      l.add(new Subscriber(row.getString("uid"), row.getString("platform"),
        row.getString("os"), row.getString("model"),
        row.getString("phone"),row.getString("device_uuid"), row.getString("email"),
        row.getString("fb_id"), row.getString("state"),
        row.getString("region"), row.getString("country"), row.getString("apps")))
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
    ps = session.prepare("UPDATE spacerock.users SET device_uuid = ?, platform = ?, os = ?, " +
      "model = ?, phone = ? WHERE uid = ?;")
    pStatements.put("ChangeDevice", ps)

    // Add user information
    ps = session.prepare("UPDATE spacerock.users SET user_name = ?, first_name = ?, last_name = ?, " +
      "email = ?, fb_id = ?, state = ?, " +
      "region = ?, country = ?, apps = ?, registered_time = ?, last_seen = ? WHERE uid = ?;")
    pStatements.put("AddUserInfo", ps)

    // add device information
    ps = session.prepare("INSERT INTO spacerock.users (uid, device_uuid, platform, os, model, phone, registered_time) " +
      "VALUES " +
      "(?, ?, ?, ?, ?, ?, ?)")
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

  def close() = {
    if (cluster != null)
      cluster.close()
  }
}

