package spacerock.persistence.cassandra



import com.datastax.driver.core._
import models.CategoryModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable
import models.CategoryModelGame
import models.GameSessionModel
import models.UserGameSessionModel



trait UserGameSession {
  def addNewUserGameSession(uid: String, gameSessionId: String): Boolean
  def addNewGameSession(uid: String, gameSessionId: String): Boolean
  def removeGameSession(uid: String, gameSessionId: String): Boolean
  def getAllGameSessionsByUid(uid : Int): List[GameSessionModel]
  def lastError: Int
}

class UserGameSessionDAO (implicit inj: Injector) extends UserGameSession with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Add new game session to user_game_sessions
   * @param uid
   * @param gameSessionId
   * @return true if success, false otherwise
   */
  override def addNewUserGameSession(uid: String, gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewUserGameSession", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val set: mutable.HashSet[String] = new mutable.HashSet[String]
    set.add(gameSessionId)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setSet("game_session_ids", set)

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }


  /**
   * Add user_game_sessions with a new game_session_id to an existing row
   * @param uid
   * @param gameSessionId
   * @return true if update successfully, false otherwise
   */
  override def addNewGameSession(uid: String, gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddGameSessionId", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val set: mutable.HashSet[String] = new mutable.HashSet[String]
    set.add(gameSessionId)
    bs.setString(0, uid)
    bs.setSet(1, set)

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }

   /**
   * Remove a game_session_id from a row in user_game_sessions
   * @param uid
   * @param gameSessionId
   * @return true if update successfully, false otherwise
   */
  override def removeGameSession(uid: String, gameSessionId: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("RemoveGameSessionId", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    
    val bs: BoundStatement = new BoundStatement(ps)
    val set: mutable.HashSet[String] = new mutable.HashSet[String]
    set.add(gameSessionId)
    bs.setString(0, uid)
    bs.setSet(1, set)

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
  override def getUserGameSessionsByUid(uid: String): UserGameSessionModel = {
    val ps: PreparedStatement = pStatements.get("GetGameSessionByUid").getOrElse(null)
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
      val row: Row = result.one()
      if (row != null) {
        val cat: UserGameSessionModel = new UserGameSessionModel(row.getString("uid"),
                                                                 row.getSet("game_session_ids", classOf[Integer]).toList.map(i => i * 1))
        cat
      } else {
        null
      }
    }
  }

  

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS

    // Add new user_game_sessions
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.user_game_sessions (uid, game_session_ids) VALUES (?, ?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("AddNewUserGameSession", ps)
    else
      _lastError = sessionManager.lastError
      
    //Update by adding a new game_session into an existing row  
    ps = sessionManager.prepare("UPDATE spacerock.user_game_sessions SET " +
      "game_session_ids = game_session_ids + ? where uid = ?;")
    if (ps != null)
      pStatements.put("AddGameSessionId", ps)
    else
      _lastError = sessionManager.lastError

    //Update by removing a game_session into an existing row  
    ps = sessionManager.prepare("UPDATE spacerock.user_game_sessions SET " +
      "game_session_ids = game_session_ids - ? where uid = ?;")
    if (ps != null)
      pStatements.put("RemoveGameSessionId", ps)
    else
      _lastError = sessionManager.lastError

    // Get category info
    ps = sessionManager.prepare("SELECT * from spacerock.user_game_sessions where uid = ?;")
    if (ps != null)
      pStatements.put("GetGameSessionByUid", ps)
    else
      _lastError = sessionManager.lastError

  }

}
