package spacerock.persistence.cassandra

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet}
import models.AchievementModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._

/**
 * Created by william on 4/22/15.
 */
trait Achievement {
  def addNewAchievement(achId: Int, description: String): Boolean
  def getAchById(achId: Int): AchievementModel

  def lastError: Int
}
class AchievementDAO (implicit inj: Injector) extends Achievement with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add new piece
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.achievement SET " +
      "description = ? WHERE achievement_id = ?;")
    if (ps != null)
      pStatements.put("AddNewAch", ps)
    else
      _lastError = sessionManager.lastError

    // Get game result by uid
    ps = sessionManager.prepare("SELECT * from spacerock.achievement where achievement_id = ?;")
    if (ps != null)
      pStatements.put("GetAchById", ps)
    else
      _lastError = sessionManager.lastError
  }

  override def addNewAchievement(achId: Int, description: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewAch", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("description", description)
    bs.setInt("achievement_id", achId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getAchById(achId: Int): AchievementModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAchById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt(0, achId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new AchievementModel(row.getInt("achievement_id"),
            row.getString("description"))
        }
      }
    }
    null
  }
}
