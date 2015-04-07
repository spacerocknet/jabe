package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.GameResultModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 1/23/15.
 */

trait GameResult {
  def addResults(uid: String, gid: Int, level: Int, score: Long): Boolean
  def addResults(grm: GameResultModel): Boolean
  def getResultsByUid(uid: String): List[GameResultModel]
  def getResultsByUidOfGame(uid: String, gameId: Int): Map[Int, Long]
  def getResultsByGameLevel(gameId: Int, level: Int): Map[String, Long]

  def lastError: Int
}

class GameResultDAO (implicit inj: Injector) extends GameResult with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Add new results from what user played
   * @param uid user id
   * @param gid game id that user played
   * @param level game level that user played
   * @param score score of playing game level
   * @return true if success, otherwise false
   */
  override def addResults(uid: String, gid: Int, level: Int, score: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddResults", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gid)
    bs.setInt("level", level)
    bs.setLong("score", score)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Add new game by game result model
   * @param grm game result model
   * @return true if success, otherwise false
   */
  override def addResults(grm: GameResultModel): Boolean = {
    addResults(grm.uid, grm.gid, grm.level, grm.score)
  }

  /**
   * get all results of all game level that user played by user id
   * @param uid
   * @return list of game result model
   */
  def getResultsByUid(uid: String): List[GameResultModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultsByUid", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, uid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[GameResultModel] = new ListBuffer[GameResultModel]
      for (row <- result.all()) {
        if (row != null) {
          l.add(new GameResultModel(row.getInt("game_id"),
            row.getInt("level"),
            row.getLong("score"),
            uid))
        }
      }
      l.toList
    }
  }

  /**
   * Get all results by game level. This method will be used in case of select top user of game and level
   * @param gameId game id
   * @param level game level
   * @return map of user id and game level score
   */
  override def getResultsByGameLevel(gameId: Int, level: Int): Map[String, Long] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultByGameLevel", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("game_id", gameId)
    bs.setInt("level", level)

    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val map: scala.collection.mutable.HashMap[String, Long] = new scala.collection.mutable.HashMap[String, Long]
      for (row: Row <- result.all()) {
        if (row != null) {
          map.put(row.getString("uid"), row.getLong("score"))
        }
      }
      map.toMap
    }
  }

  /**
   * Get all results by uid and game id of all time. It will return a map of level by score
   * @param uid user id
   * @param gameId game id
   * @return map of level and score
   */
  override def getResultsByUidOfGame(uid: String, gameId: Int): Map[Int, Long] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultByUidGame", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val map: scala.collection.mutable.HashMap[Int, Long] = new scala.collection.mutable.HashMap[Int, Long]
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("game_id", gameId)
    bs.setString("uid", uid)

    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row: Row <- result.all()) {
        if (row != null) {
          map.put(row.getInt("level"), row.getLong("score"))
        }
      }
      map.toMap
    }
  }

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add Game result
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.game_result SET score = ? " +
      "WHERE game_id = ? AND level = ? and uid = ?;")
    if (ps != null)
      pStatements.put("AddResults", ps)
    else
      _lastError = sessionManager.lastError

    // Get game result by uid
    ps = sessionManager.prepare("SELECT * from spacerock.game_result where uid = ?;")
    if (ps != null)
      pStatements.put("GetResultsByUid", ps)
    else
      _lastError = sessionManager.lastError

    // Get game result by game id and level;
    ps = sessionManager.prepare("SELECT score, uid from spacerock.game_result where game_id = ? and level = ?;")
    if (ps != null)
      pStatements.put("GetResultByGameLevel", ps)
    else
      _lastError = sessionManager.lastError

    // Get game result by user id and game id;
    ps = sessionManager.prepare("SELECT score, level from spacerock.game_result where game_id = ? and uid = ?;")
    if (ps != null)
      pStatements.put("GetResultByUidGame", ps)
    else
      _lastError = sessionManager.lastError

  }
}
