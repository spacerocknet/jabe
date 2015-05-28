package spacerock.persistence.cassandra

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.datastax.driver.core._

import models.GameSessionModel
import play.Logger
import scaldi.Injectable
import scaldi.Injector
import spacerock.constants.Constants


trait GameSession {
  def getGameSessionById(gameSessionId: String): GameSessionModel
  def addNewGameSession(gameSessionId: String, uid1: String): Boolean
  def updateGameSessionOnOpponent(gameSessionId: String, uid2: String): Boolean
  def updateGameSession(gameSessionId: String, uid: String, puzzlePieces: Int): Boolean

  def lastError: Int
}

class GameSessionDAO (implicit inj: Injector) extends GameSession with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Add a new game session
   * @param category category name
   * @param gameSessionId and a uid
   * @return true if success, false otherwise
   */
  override def addNewGameSession(gameSessionId: String, uid1: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddGameSession", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("game_session_id", gameSessionId)
    bs.setString("uid_1", uid1)
    bs.setInt("puzzle_pieces_1", 1000000000)
    bs.setLong("uid_1_last_move", System.currentTimeMillis())
    bs.setInt("current_turn", 1);
    bs.setInt("current_round", 0)
    
    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }


  /**
   * Update category with game id list and description
   * @param category
   * @param gameId
   * @param description
   * @return true if update successfully, false otherwise
   */
  override def updateGameSessionOnOpponent(gameSessionId: String, uid2: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateOpponent", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)

    //TODOs: find the row first and the update

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }




  
    /**
   * Get all categories from system for a specified game id
   * @return list of categories
   */
   def getGameSessionById(gameSessionId: String): GameSessionModel = {
    //TODOs: filtering out game id from the results
    val ps: PreparedStatement = pStatements.getOrElse("GetGameSessionById", null)
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
      val r : Row = result.one()
      
      if (r != null) {
          return new GameSessionModel(r.getString("game_session_id"),
                                     r.getInt("state"),
                                     r.getString("uid1"),
                                     r.getInt("puzzle_pieces_1"),
                                     r.getLong("uid_1_last_move"),
                                     r.getString("uid2"),
                                     r.getInt("puzzle_pieces_2"),
                                     r.getLong("uid_2_last_move"),
                                     r.getInt("current_turn"),
                                     r.getInt("current_round"))
                       
        
      } 
      
      null
    }
  }


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // update category
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.UpdateOpponent SET uid_2 = ?, puzzle_pieces_2 = ?, uid_2_last_move = ?, current_turn = 2, current_round = current_round + 1 " +
                                                       "where game_session_id = ?;")
    if (ps != null)
      pStatements.put("UpdateOpponent", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.UpdateOpponent SET uid_1 = ?, puzzle_pieces_1 = ?, uid_1_last_move = ?, current_turn = 1, current_round = current_round + 1 " +
                                "where game_session_id = ?;")
    if (ps != null)
      pStatements.put("UpdateInitiator", ps)
    else
      _lastError = sessionManager.lastError

    // Add new category
    ps = sessionManager.prepare("INSERT INTO spacerock.game_sessions (game_session_id, uid_1, puzzle_pieces_1, uid_1_last_move, current_turn, current_round) " +
                                " VALUES (?, ?, ?, ?, ?, ?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("AddGameSession", ps)
    else
      _lastError = sessionManager.lastError

    // Get category info
    ps = sessionManager.prepare("SELECT * from spacerock.game_sessions where game_session_id = ?;")
    if (ps != null)
      pStatements.put("GetGameSessionById", ps)
    else
      _lastError = sessionManager.lastError

  }

}
