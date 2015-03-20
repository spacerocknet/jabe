package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.GameModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._

/**
 * Created by william on 1/13/15.
 */

trait GameInfo {
  def getGameInfoByGid(gid: Int): GameModel
  def getGameInfoByName(gName: String): GameModel
  def addGameInfo(game: GameModel): Boolean
  def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
                  bgp: Int): Boolean
  def updateGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
    bgp: Int): Boolean
  def updateGameInfo(game: GameModel): Boolean
  def getAllGames: List[GameModel]
  def lastError: Int
}

class GameInfoDAO (implicit inj: Injector) extends GameInfo with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
              = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  /**
   * Get game's information by game id
   * @param gid game id
   * @return game information (if success) or null (if not)
   */
  override def getGameInfoByGid(gid: Int): GameModel = {
    val ps: PreparedStatement = pStatements.get("GetGameInfoById").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        val game: GameModel = new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
          row.getSet("categories", classOf[String]).toSet,
          row.getInt("battles_per_game"))
        game
      } else {
        return null
      }
    }
  }

  /**
   * Get game's information by game name.
   * @param gName game name
   * @return game information (if success) or null (if not)
   */
  override def getGameInfoByName(gName: String): GameModel = {
    val ps: PreparedStatement = pStatements.get("GetGameInfoByName").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("game_name", gName)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        val game: GameModel = new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
          row.getSet("categories", classOf[String]).toSet,
          row.getInt("battles_per_game"))
        game
      } else {
        null
      }
    }
  }

  /**
   * Add new game to system.
   * @param game game model
   * @return true if success, otherwise false
   */
  override def addGameInfo(game: GameModel): Boolean = {
    addGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }
  override def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
                  bgp: Int): Boolean = {
    val ps: PreparedStatement = pStatements.get("AddGameInfo").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    bs.setString("game_name", gameName)
    bs.setString("game_description", gameDescription)
    bs.setSet("categories", categories)
    bs.setInt("battles_per_game", bgp)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Update game info
   * @param gId game id
   * @param gameName game name
   * @param gameDescription game description
   * @param categories game's categories
   * @param bgp battles per game
   * @return true if success, otherewise false
   */
  override def updateGameInfo(gId: Int, gameName: String, gameDescription: String, categories: Set[String],
                     bgp: Int): Boolean = {
    val ps: PreparedStatement = pStatements.get("UpdateGameInfo").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setSet("categories", categories)
    bs.setString("game_description", gameDescription)
    bs.setString("game_name", gameName)
    bs.setInt("battles_per_game", bgp)
    bs.setInt("gid", gId)
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Update game info by game model
   * @param game game model
   * @return true if sucess, otherwise false
   */
  override def updateGameInfo(game: GameModel): Boolean = {
    updateGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }

  /**
   * Get all game of the system
   * @return list of game models
   */
  override def getAllGames(): List[GameModel] = {
    val ps: PreparedStatement = pStatements.get("GetAllGames").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }

    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: scala.collection.mutable.ListBuffer[GameModel] = scala.collection.mutable.ListBuffer()
      for (row <- result.all()) {
        if (row != null) {
          l.add(new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
            row.getSet("categories", classOf[String]).toSet,
            row.getInt("battles_per_game")))
        }
      }
      l.toList
    }
  }

  def init() = {
    // get game info by id
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    var ps: PreparedStatement = sessionManager.prepare("SELECT * from spacerock.game_info where gid = ?;")
    if (ps != null)
      pStatements.put("GetGameInfoById", ps)
    else
      _lastError = sessionManager.lastError

    // insert new game
    ps = sessionManager.prepare("INSERT INTO spacerock.game_info (gid, game_name, game_description, categories, battles_per_game) " +
      "VALUES (?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddGameInfo", ps)
    else
      _lastError = sessionManager.lastError

    // get game info by game name
    ps = sessionManager.prepare("SELECT * from spacerock.game_info where game_name = ? ALLOW FILTERING;")
    if (ps != null)
      pStatements.put("GetGameInfoByName", ps)
    else
      _lastError = sessionManager.lastError

    // Get all game
    ps = sessionManager.prepare("SELECT * FROM spacerock.game_info ALLOW FILTERING;")
    if (ps != null)
      pStatements.put("GetAllGames", ps)
    else
      _lastError = sessionManager.lastError

    // Get all game
    ps = sessionManager.prepare("UPDATE spacerock.game_info SET categories = categories + ?, " +
      "game_description = ?, game_name = ?, battles_per_game = ? WHERE gid = ?;")
    if (ps != null)
      pStatements.put("UpdateGameInfo", ps)
    else
      _lastError = sessionManager.lastError
  }

}
