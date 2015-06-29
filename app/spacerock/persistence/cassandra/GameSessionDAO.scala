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
  def getGameSessionsByIds(gameSessionIds : List[String]): List[GameSessionModel] 
  def addNewGameSession(gameSessionId: String, uid1: String): GameSessionModel
  //def updateGameSessionOnOpponent(gameSessionId: String, uid2: String): Boolean
  def updateGameSessionOnPlayer(gameSessionId: String, uid: String, puzzlePieces: Int, changeTurn : Boolean): GameSessionModel
  def updateGameSessionState(gameSessionId: String, state: Int): Boolean
  def removeGameSession(gameSessionId: String): Boolean
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
  override def addNewGameSession(gameSessionId: String, uid1: String): GameSessionModel = {
    val ps: PreparedStatement = pStatements.getOrElse("AddGameSession", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val ts = System.currentTimeMillis()
    bs.setString("game_session_id", gameSessionId)
    bs.setString("uid_1", uid1)
    bs.setInt("puzzle_pieces_1", 1000000000)
    bs.setLong("uid_1_last_move", ts)
    bs.setInt("current_turn", 1);
    bs.setInt("current_round", 0)
    
    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      return null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      return new GameSessionModel(gameSessionId, 0, 
                                  uid1, 1000000000, ts,
                                  "", 1000000000, ts-1,
                                  1, 0)
    }
  }


  /**
   * Update game_session by game_session_id and uid 
   * @param gameSessionId
   * @param uid
   * @param puzzlePieces
   * @param changeTurn
   * @return true if update successfully, false otherwise
   */
  override def updateGameSessionOnPlayer(gameSessionId: String, uid: String, puzzlePieces: Int, changeTurn : Boolean): GameSessionModel = {
    val gameSession = getGameSessionById(gameSessionId)
    if (gameSession == null) {
         Logger.info("Unable to find gameSessionId: " + gameSessionId + " in game_sessions!!!")
         return null
    }
    
    var ps: PreparedStatement = null
    var bs: BoundStatement = null
    if (gameSession.uid1 == uid) {
       ps = pStatements.getOrElse("UpdateGameSessionPlayer1", null)    
       if (ps == null || !sessionManager.connected) {
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
          Logger.error("Cannot connect to database")
          return null
       }
       val lastMove : Long = System.currentTimeMillis()
       bs = new BoundStatement(ps)
       bs.setString(0, uid)
       bs.setInt(1, puzzlePieces)
       bs.setLong(2, lastMove)
       bs.setInt(3, gameSession.currentRound + 1)
       
       if (changeTurn) {
          bs.setInt(4, 2)
          gameSession.currentTurn = 2
       } else { 
          bs.setInt(4, 1)
          gameSession.currentTurn = 1
       }
       
       bs.setString(5, gameSessionId)
       
       gameSession.uid1 = uid
       gameSession.puzzlePieces1 = puzzlePieces
       gameSession.uid1LastMove = lastMove
       gameSession.currentRound = gameSession.currentRound + 1
       
    } else {
       ps = pStatements.getOrElse("UpdateGameSessionPlayer2", null) 
       if (ps == null || !sessionManager.connected) {
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
          Logger.error("Cannot connect to database")
          return null
       }
       val lastMove = System.currentTimeMillis()
       bs = new BoundStatement(ps)
       bs.setString(0, uid)
       bs.setInt(1, puzzlePieces)
       bs.setLong(2, lastMove)
       bs.setInt(3, gameSession.currentRound + 1)
       if (changeTurn) {
          bs.setInt(4, 1)
          gameSession.currentTurn = 1
       } else  {
          bs.setInt(4, 2)
          gameSession.currentTurn = 2
       }   
       bs.setString(5, gameSessionId)
       
       gameSession.uid2 = uid
       gameSession.puzzlePieces2 = puzzlePieces
       gameSession.uid2LastMove = lastMove
       gameSession.currentRound = gameSession.currentRound + 1
    }
   
    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      return gameSession
    }
  }


  /**
   * Update game_session's state
   * @param state
   * @param gameId
   * @return true if update successfully, false otherwise
   */
   def updateGameSessionState(gameSessionId: String, state: Int): Boolean = {
     val ps: PreparedStatement = pStatements.getOrElse("UpdateGameSessionState", null)
     if (ps == null || !sessionManager.connected) {
       _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
       Logger.error("Cannot connect to database")
       return false
     }
     val bs: BoundStatement = new BoundStatement(ps)

     bs.setString("game_session_id", gameSessionId)
     bs.setInt("state", state)

     if (sessionManager.execute(bs) == null) {
       _lastError = sessionManager.lastError
       false
     } else {
       _lastError = Constants.ErrorCode.ERROR_SUCCESS
       true
     }
     
     //val batchSt: BatchStatement = new BatchStatement()
     //batchSt.add(bs)
     //sessionManager.execute(batchSt)
     //true
  }

  
    /**
   * Get all categories from system for a specified game id
   * @return list of categories
   */
   override def getGameSessionById(gameSessionId: String): GameSessionModel = {
    //TODOs: filtering out game id from the results
    val ps: PreparedStatement = pStatements.getOrElse("GetGameSessionById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, gameSessionId)

    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      for (r: Row <- result.all()) {
        if (r != null) {
          return new GameSessionModel(r.getString("game_session_id"),
                                     r.getInt("state"),
                                     r.getString("uid_1"),
                                     r.getInt("puzzle_pieces_1"),
                                     r.getLong("uid_1_last_move"),
                                     r.getString("uid_2"),
                                     r.getInt("puzzle_pieces_2"),
                                     r.getLong("uid_2_last_move"),
                                     r.getInt("current_turn"),
                                     r.getInt("current_round"))
        }
      }
      
      null
    }
  }
   
   
   def getGameSessionsByIds(gameSessionIds : List[String]): List[GameSessionModel] = {
       val ps: PreparedStatement = pStatements.getOrElse("GetGameSessionById", null)
       if (ps == null || !sessionManager.connected) {
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
           Logger.error("Cannot connect to database")
           return null
       }
       
       val retVal: scala.collection.mutable.ListBuffer[GameSessionModel] = scala.collection.mutable.ListBuffer()
        
       for(gameSessionId <- gameSessionIds) {
         val bs: BoundStatement = new BoundStatement(ps)
         bs.setString(0, gameSessionId)
                
         val result: ResultSet = sessionManager.execute(bs)

         for (r: Row <- result.all()) {
             retVal.add(new GameSessionModel(r.getString("game_session_id"),
                                     r.getInt("state"),
                                     r.getString("uid_1"),
                                     r.getInt("puzzle_pieces_1"),
                                     r.getLong("uid_1_last_move"),
                                     r.getString("uid_2"),
                                     r.getInt("puzzle_pieces_2"),
                                     r.getLong("uid_2_last_move"),
                                     r.getInt("current_turn"),
                                     r.getInt("current_round")))
         }
      }
       
      return retVal.toList 
   }
   
   /**
   * Remove a game_session_id from a row in game_sessions
   * @param uid
   * @param gameSessionId
   * @return true if update successfully, false otherwise
   */
  override def removeGameSession(gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("DeleteGameSessionById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("game_session_id", gameSessionId)

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  } 

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS

    //update game_session on the player 1
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.game_sessions SET uid_1 = ?, puzzle_pieces_1 = ?, uid_1_last_move = ?, current_round = ?, current_turn = ?  " +
                                "where game_session_id = ?;")
    if (ps != null)
      pStatements.put("UpdateGameSessionPlayer1", ps)
    else
      _lastError = sessionManager.lastError

    // update game_session on the player 2
    ps = sessionManager.prepare("UPDATE spacerock.game_sessions SET uid_2 = ?, puzzle_pieces_2 = ?, uid_2_last_move = ?, current_round = ?, current_turn = ? " +
                                                       "where game_session_id = ?;")
    if (ps != null)
      pStatements.put("UpdateGameSessionPlayer2", ps)
    else
      _lastError = sessionManager.lastError
      
      
    //update game_session's state
    ps = sessionManager.prepare("UPDATE spacerock.game_sessions SET state = ? where game_session_id = ?;")
    if (ps != null)
      pStatements.put("UpdateGameSessionState", ps)
    else
      _lastError = sessionManager.lastError
      
      
    // Add new game_session
    ps = sessionManager.prepare("INSERT INTO spacerock.game_sessions (game_session_id, uid_1, puzzle_pieces_1, uid_1_last_move, current_turn, current_round) " +
                                " VALUES (?, ?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddGameSession", ps)
    else
      _lastError = sessionManager.lastError

    // Get game_session info
    ps = sessionManager.prepare("SELECT * from spacerock.game_sessions where game_session_id = ?;")
    if (ps != null)
      pStatements.put("GetGameSessionById", ps)
    else
      _lastError = sessionManager.lastError
      
    //delete a game_session
    ps = sessionManager.prepare("DELETE from spacerock.game_sessions where game_session_id = ?;")
    if (ps != null)
      pStatements.put("DeleteGameSessionById", ps)
    else
      _lastError = sessionManager.lastError
      
  }

}
