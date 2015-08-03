package spacerock.persistence.cassandra

import java.util

import com.datastax.driver.core._
import models.SubscriberModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 1/13/15.
 */

trait UserData {
  def getInfoByUID(uuid: String): SubscriberModel
  def getInfoByUsername(userName: String): List[SubscriberModel]
  def addBasicInfo(uid: String, userName: String, firstName: String, lastName: String,
                      email: String, fbId: String, locState: String, locRegion: String,
                      locCountry: String, appName: String): Boolean
  def addDeviceInfo(uid: String, platform: String, os: String, model: String,
                    phone: String, deviceUuid: String): Boolean
  def addDeviceInfo(subscriber: SubscriberModel): Boolean
  def updateLastSeenField(uuid: String): Boolean
  def getAllUsers(): List[SubscriberModel]
  def changeDevice(uid: String, platform: String, os: String, model: String,
                   phone: String, deviceUuid: String): Boolean
  def delAllUsers(): Boolean
  def lastError: Int
}

class UserDataDAO (implicit inj: Injector) extends UserData with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
        = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Update last seen field on users table
   * @param uid user id
   * @return true if success, otherwise false
   */
  override def updateLastSeenField(uid: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateLastSeen", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setLong("last_seen", System.currentTimeMillis())
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val time: Long = System.currentTimeMillis()

    bs.setString("uid", uid)

    if (userName != null)
      bs.setString("user_name", userName)
    else
      bs.setString("user_name", "")

    if (firstName != null)
      bs.setString("first_name", firstName)
    else
      bs.setString("first_name", "")

    if (lastName != null)
      bs.setString("last_name", lastName)
    else
      bs.setString("last_name", "")

    if (email != null)
      bs.setString("email", email)
    else
      bs.setString("email", "")

    if (fbId != null)
      bs.setString("fb_id", fbId)
    else
      bs.setString("fb_id", "")

    if (locState != null)
      bs.setString("state", locState)
    else
      bs.setString("state", "")

    if (locRegion != null)
      bs.setString("region", locRegion)
    else
      bs.setString("region", locRegion)

    if (locCountry != null)
       bs.setString("country", locCountry)
    else
      bs.setString("country", locCountry)

    if (appName != null)
      bs.setString("apps", appName)
    else
      bs.setString("apps", "")


    bs.setLong("last_seen", time)

    bs.setLong("registered_time", time)

    if (sessionManager.execute(bs) != null) {
      if (updateUidName(uid, userName)) {
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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
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
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
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
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        Logger.info("Justin here 1")
        Logger.info(row.toString())
        val subscriber: SubscriberModel = new SubscriberModel(row.getString("uid"),
                                               row.getString("platform"),
                                               row.getString("os"),
                                               row.getString("model"),
                                               row.getString("phone"),
                                               row.getString("device_uuid"),
                                               row.getString("email"),
                                               row.getString("fb_id"),
                                               row.getString("state"),
                                               row.getString("region"),
                                               row.getString("country"),
                                               row.getString("apps"))


        subscriber.userName = if (row.getString("user_name") == null) "" else row.getString("user_name")
        subscriber.deviceSet = row.getSet("device_list", classOf[String]).toSet
        Logger.info(subscriber.toString())
        subscriber
      } else {
        null
      }
    } else {
      _lastError = sessionManager.lastError
      null
    }
  }

  /**
   * Get user's information by username
   * @param userName
   * @return Subscriber model if success, otherwise null
   */
  def getInfoByUsername(userName: String): List[SubscriberModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetUserInfoByUsernameI", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("user_name", userName)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      Logger.info("Minh in 1")
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        Logger.info("Minh in 2")
        val l: ListBuffer[SubscriberModel] = new ListBuffer[SubscriberModel]
        val uids: util.Set[String] = row.getSet("uids", classOf[String])
        if (uids != null) {
          Logger.info("Minh in 3")
          Logger.info("uids = " + uids)
          for (uid <- uids) {
            val subscriber: SubscriberModel = getInfoByUID(uid)
            if (subscriber != null)
              l.add(subscriber)
          }
        }
        l.toList
      } else {
        _lastError = sessionManager.lastError
        null
      }
    } else {
      _lastError = sessionManager.lastError
      null
    }
  }

  /**
   * Get all user from system.
   * @return list of subscriber models
   */
  override def getAllUsers(): List[SubscriberModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllUsers", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: scala.collection.mutable.ListBuffer[SubscriberModel] = scala.collection.mutable.ListBuffer()
      for (row: Row <- result.all()) {
        if (row != null) {
          val sm: SubscriberModel = new SubscriberModel(row.getString("uid"), row.getString("platform"),
            row.getString("os"), row.getString("model"),
            row.getString("phone"), row.getString("device_uuid"), row.getString("email"),
            row.getString("fb_id"), row.getString("state"),
            row.getString("region"), row.getString("country"), row.getString("apps"))
          sm.userName = row.getString("user_name")
          sm.deviceSet = row.getSet("device_list", classOf[String]).toSet
          l.add(sm)
        }
      }
      l.toList
    } else {
      _lastError = sessionManager.lastError
      null
    }
  }


  def delAllUsers(): Boolean = {
    var ps: PreparedStatement = pStatements.getOrElse("DeleteAllUsers", null)
    var bs: BoundStatement = new BoundStatement(ps)
    sessionManager.execute(bs)

    ps = pStatements.getOrElse("DeleteAllUsersUsername", null)
    bs = new BoundStatement(ps)
    sessionManager.execute(bs)

    ps = pStatements.getOrElse("DeleteAllUsersGameSessions", null)
    bs = new BoundStatement(ps)
    sessionManager.execute(bs)

    ps = pStatements.getOrElse("DeleteAllUserGameProfile", null)
    bs = new BoundStatement(ps)
    sessionManager.execute(bs)

    ps = pStatements.getOrElse("DeleteAllOpenGameSessions", null)
    bs = new BoundStatement(ps)
    sessionManager.execute(bs)

    ps = pStatements.getOrElse("DeleteAllGameSessions", null)
    bs = new BoundStatement(ps)
    sessionManager.execute(bs)

    true
  }


  private def updateUidName(uid: String, uname: String): Boolean = {
      val ps: PreparedStatement = pStatements.getOrElse("UpdateUidNameI", null)
      if (ps == null || !sessionManager.connected) {
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
        Logger.error("Cannot connect to database")
        return false
      }
      val bs: BoundStatement = new BoundStatement(ps)
      bs.setSet("uids", Set[String]{uid})
      bs.setString("user_name", uname)
      if (sessionManager.execute(bs) != null) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        _lastError = sessionManager.lastError
        false
      }
  }

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // update last seen
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.users SET last_seen = ? where uid = ?;")
    if (ps != null)
      pStatements.put("UpdateLastSeen", ps)
    else
      _lastError = sessionManager.lastError

    // change user device
    ps = sessionManager.prepare("UPDATE spacerock.users SET device_list = device_list + ?, device_uuid = ?, platform = ?, os = ?, " +
      "model = ?, phone = ? WHERE uid = ?;")
    if (ps != null)
      pStatements.put("ChangeDevice", ps)
    else
    _lastError = sessionManager.lastError

    // Add user information
    ps = sessionManager.prepare("UPDATE spacerock.users SET user_name = ?, first_name = ?, last_name = ?, " +
      "email = ?, fb_id = ?, state = ?, " +
      "region = ?, country = ?, apps = ?, registered_time = ?, last_seen = ? WHERE uid = ?;")
    if (ps != null)
      pStatements.put("AddUserInfo", ps)
    else
    _lastError = sessionManager.lastError

    // update inverse table user_username
    ps = sessionManager.prepare("UPDATE spacerock.user_username SET uids = uids + ? " +
      "WHERE user_name = ?;")
    if (ps != null)
      pStatements.put("UpdateUidNameI", ps)
    else
      _lastError = sessionManager.lastError

    // add device information

      ps = sessionManager.prepare("INSERT INTO spacerock.users (uid, device_uuid, platform, os, " +
      "model, phone, registered_time, device_list) " +
      "VALUES " +
      "(?, ?, ?, ?, ?, ?, ?, ?)")
    if (ps != null)
      pStatements.put("AddDeviceInfo", ps)
    else
    _lastError = sessionManager.lastError

    // Get user info
    ps = sessionManager.prepare("SELECT * from spacerock.users where uid = ?;")
    if (ps != null)
      pStatements.put("GetUserInfoByUID", ps)
    else
    _lastError = sessionManager.lastError

    ps = sessionManager.prepare("SELECT uids from spacerock.user_username where user_name = ?;")
    if (ps != null)
      pStatements.put("GetUserInfoByUsernameI", ps)
    else
    _lastError = sessionManager.lastError

    // Get all users
    ps = sessionManager.prepare("SELECT * FROM spacerock.users ALLOW FILTERING;")
    if (ps != null)
      pStatements.put("GetAllUsers", ps)
    else
    _lastError = sessionManager.lastError

    //Delete all users
    ps = sessionManager.prepare("truncate spacerock.users")
    if (ps != null)
      pStatements.put("DeleteAllUsers", ps)
    else
      _lastError = sessionManager.lastError


    //Delete all users_username
    ps = sessionManager.prepare("truncate spacerock.user_username")
    if (ps != null)
      pStatements.put("DeleteAllUsersUsername", ps)
    else
      _lastError = sessionManager.lastError

    //Delete all user_game_sessions
    ps = sessionManager.prepare("truncate spacerock.user_game_sessions")
    if (ps != null)
      pStatements.put("DeleteAllUsersGameSessions", ps)
    else
      _lastError = sessionManager.lastError


    //Delete all user_game_profile
    ps = sessionManager.prepare("truncate spacerock.user_game_profile")
    if (ps != null)
      pStatements.put("DeleteAllUserGameProfile", ps)
    else
      _lastError = sessionManager.lastError


    //Delete all open_game_sessions
    ps = sessionManager.prepare("truncate spacerock.open_game_sessions")
    if (ps != null)
      pStatements.put("DeleteAllOpenGameSessions", ps)
    else
      _lastError = sessionManager.lastError


    //Delete all game_sessions
    ps = sessionManager.prepare("truncate spacerock.game_sessions")
    if (ps != null)
      pStatements.put("DeleteAllGameSessions", ps)
    else
      _lastError = sessionManager.lastError
  }
}
