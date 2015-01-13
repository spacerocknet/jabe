package spacerock.persistence

import java.util.UUID

import com.datastax.driver.core._
import models.Subscriber
import scaldi.{Injectable, Injector}

/**
 * Created by william on 1/13/15.
 */

trait NewAppConfig {
  def getUserInfoByUID(uuid: UUID): Subscriber
  def getUserInfoByUsername(userName: String): Subscriber
  def addUserInfo(uuid: UUID, userName: String, firstName: String, lastName: String,
                  email: String, fbId: String, locState: String, locRegion: String,
                  appName: String): Boolean
  def updateLastSeenField(uuid: UUID): Boolean
  def getAllUsers(): List[Subscriber]
}

class NewAppConfigDAO (implicit inj: Injector) extends NewAppConfig with Injectable{
//  val clusterName = inject [String] (identified by "cassandra.cluster")
//  var cluster: Cluster = null
//  var session: Session = null
//  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
//  = scala.collection.mutable.Map[String, PreparedStatement]()
//
//  val isConnected: Boolean = connect("127.0.0.1")
//
//  override def updateLastSeenField(uuid: UUID): Boolean = {
//    val ps: PreparedStatement = pStatements.getOrElse("UpdateLastSeen", null)
//    val bs: BoundStatement = new BoundStatement(ps)
//    bs.setUUID("uid", uuid)
//    bs.setLong("last_seen", System.currentTimeMillis())
//    val result: ResultSet = session.execute(bs)
//    println(result.one().toString)
//    true
//  }
//
//  override def addUserInfo(uuid: UUID, userName: String, firstName: String, lastName: String,
//                           email: String, fbId: String, locState: String, locRegion: String,
//                           appName: String): Boolean = {
//    val ps: PreparedStatement = pStatements.getOrElse("AddNewUser", null)
//    val bs: BoundStatement = new BoundStatement(ps)
//    val time: Long = System.currentTimeMillis()
//    bs.setUUID("uid", uuid)
//    bs.setString("user_name", userName)
//    bs.setString("first_name", firstName)
//    bs.setString("last_name", lastName)
//    bs.setString("email", email)
//    bs.setString("fb_id", fbId)
//    bs.setString("loc_state", locState)
//    bs.setString("loc_region", locRegion)
//    bs.setString("app_name", appName)
//    bs.setLong("last_seen", time)
//    bs.setLong("registered_time", time)
//    session.execute(bs)
//    true
//  }
//
//  override def getUserInfoByUID(uuid: UUID): Subscriber = {
//    val ps: PreparedStatement = pStatements.get("GetUserInfoByUID").getOrElse(null)
//
//    val bs: BoundStatement = new BoundStatement(ps)
//    bs.setUUID("uid", uuid)
//    val result: ResultSet = session.execute(bs)
//    val row: Row = result.one()
//    val subscriber: Subscriber = new Subscriber(row.getUUID("uid").toString, row.getString("first_name"),
//      row.getString("last_name"), row.getString("user_name"),
//      row.getString("email"))
//    subscriber
//  }
//
//  def getUserInfoByUsername(userName: String): Subscriber = {
//    val ps: PreparedStatement = pStatements.getOrElse("GetUserInfoByUsername", null)
//
//    val bs: BoundStatement = new BoundStatement(ps)
//    bs.setString("user_name", userName)
//    val result: ResultSet = session.execute(bs)
//    val row: Row = result.one()
//    val subscriber: Subscriber = new Subscriber(row.getUUID("uid").toString, row.getString("first_name"),
//      row.getString("last_name"), row.getString("user_name"),
//      row.getString("email"))
//    subscriber
//  }
//
//  override def getAllUsers(): List[Subscriber] = {
//    val ps: PreparedStatement = pStatements.getOrElse("GetAllUsers", null)
//    val bs: BoundStatement = new BoundStatement(ps)
//    val result: ResultSet = session.execute(bs)
//    val l: scala.collection.mutable.ListBuffer[Subscriber] = scala.collection.mutable.ListBuffer()
//    for (e: Row <- result.all()) {
//      l.add(new Subscriber(e.getUUID("uid").toString(), e.getString("first_name"),
//        e.getString("last_name"), e.getString("user_name"),
//        e.getString("email")))
//    }
//    l.toList
//  }
//
//  def connect(node: String): Boolean = {
//    cluster = Cluster.builder().addContactPoint(node).build()
//    val metadata = cluster.getMetadata()
//    var countHost: Int = 0
//    metadata.getAllHosts() map {
//      case host => countHost += 1
//    }
//    session = cluster.connect()
//
//    if (countHost < 1)
//      false
//    else {
//      init()
//      true
//    }
//  }
//
//  def init() = {
//    // update last seen
//    var ps: PreparedStatement = session.prepare("UPDATE spacerock.users SET last_seen = ? where uid = ?;")
//    pStatements.put("UpdateLastSeen", ps)
//
//    // Add new user
//    ps = session.prepare("INSERT INTO spacerock.users (uid, user_name, first_name, last_name, email, fb_id, loc_state, " +
//      "loc_region, app_name, registered_time, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
//    pStatements.put("AddNewUser", ps)
//
//    // Get user info
//    ps = session.prepare("SELECT * from spacerock.users where uid = ?;")
//    pStatements.put("GetUserInfoByUID", ps)
//    ps = session.prepare("SELECT * from spacerock.users where user_name = ? ALLOW FILTERING;")
//    pStatements.put("GetUserInfoByUsername", ps)
//
//    // Get all users
//    ps = session.prepare("SELECT * FROM spacerock.users ALLOW FILTERING;")
//    pStatements.put("GetAllUsers", ps)
//  }
//
//  def close() = {
//    if (cluster != null)
//      cluster.close()
//  }
  override def getUserInfoByUID(uuid: UUID): Subscriber = ???

  override def updateLastSeenField(uuid: UUID): Boolean = ???

  override def getUserInfoByUsername(userName: String): Subscriber = ???

  override def addUserInfo(uuid: UUID, userName: String, firstName: String, lastName: String, email: String, fbId: String, locState: String, locRegion: String, appName: String): Boolean = ???

  override def getAllUsers(): List[Subscriber] = ???
}
