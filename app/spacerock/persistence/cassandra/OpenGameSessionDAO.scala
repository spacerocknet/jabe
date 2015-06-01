package spacerock.persistence.cassandra



import com.datastax.driver.core._
import models.CategoryModel
import play.Logger
import scaldi.{Injectable, Injector}
import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable
import models.CategoryModelGame
import models.GameSessionModel
import models.UserGameSessionModel
import models.OpenGameSessionModel
import spacerock.constants.Constants




trait OpenGameSession {
  def addNewOpenGameSession(gameSessionId: String): Boolean
  def removeOpenGameSession(gameSessionId: String): Boolean
  def getGameSessions(limit : Int): List[OpenGameSessionModel]
  def lastError: Int
}

class OpenGameSessionDAO (implicit inj: Injector) extends OpenGameSession with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Add new open game session to open_game_sessions
   * @param uid
   * @param gameSessionId
   * @return true if success, false otherwise
   */
  override def addNewOpenGameSession(gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewOpenGameSession", null)
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


   /**
   * Remove a game_session_id from a row in open_game_sessions
   * @param uid
   * @param gameSessionId
   * @return true if update successfully, false otherwise
   */
  override def removeOpenGameSession(gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("DeleteGameSessionId", null)
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
  

  /**
   * Get category information by name. This will return an instance of found category
   * @param category category name
   * @return category model if exited or null if not found/error
   */
  override def getGameSessions(limit : Int): List[OpenGameSessionModel] = {
    val ps: PreparedStatement = pStatements.get("GetOpenGameSessions").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt(0, limit)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: scala.collection.mutable.ListBuffer[OpenGameSessionModel] = scala.collection.mutable.ListBuffer()
      for (r: Row <- result.all()) {
        if (r != null) {
          l.add(new OpenGameSessionModel(r.getString("game_session_id")))
        }
      }
      l.toList
      
    }
  }

  

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS

    // Add new user_game_sessions
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.open_game_sessions (game_session_id) VALUES (?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("AddNewOpenGameSession", ps)
    else
      _lastError = sessionManager.lastError
      
    //delete an open_game_session's row  
    ps = sessionManager.prepare("delete from spacerock.open_game_sessions where game_session_id = ?;")
    if (ps != null)
      pStatements.put("DeleteGameSessionId", ps)
    else
      _lastError = sessionManager.lastError

    // Get category info
    ps = sessionManager.prepare("SELECT * from spacerock.open_game_sessions limit ?;")
    if (ps != null)
      pStatements.put("GetOpenGameSessions", ps)
    else
      _lastError = sessionManager.lastError

  }

}
