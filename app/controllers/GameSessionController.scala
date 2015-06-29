package controllers

import models.GameSessionModel
import models.OpenGameSessionModel
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.Injectable
import scaldi.Injector
import spacerock.persistence.cassandra.GameSession
import spacerock.persistence.cassandra.OpenGameSession
import spacerock.persistence.cassandra.UserGameSession
import spacerock.usergame.GameSessionUtil
import spacerock.utils.StaticVariables

class GameSessionController (implicit inj: Injector) extends Controller with Injectable {

  val openGameSessionDao = inject[OpenGameSession]
  val gameSessionDao = inject[GameSession]
  val userGameSessionDao = inject[UserGameSession]
  
  implicit val gameSessionFmt = Json.format[GameSessionModel]


  /**
   *  Create a new game session or join an existing game session
    * @param uid : user id
   * @return json of user game session.
   */
  //TODOS: take care of error cases
  def createOrJoinAGameSession = Action {  request =>
      val json: Option[JsValue] = request.body.asJson
      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
  
      Logger.info("Received uid : " + uid)
       
      val openGameSession = GameSessionUtil.takeAnOpenGameSession(uid)
      if (openGameSession == null) {
           Ok(Json.toJson(StaticVariables.BackendErrorStatus))
      }
      
      if (openGameSession.gameSessionId == "-1" || openGameSession.gameSessionId == "-2") {
          
          Logger.info("No available open game sessions")
          //TODOs: will use a different method to generate unique gamesessionId
          val gameSessionId = "gamesession_" + uid + "_" + System.currentTimeMillis()
          val gameSessionModel = gameSessionDao.addNewGameSession(gameSessionId, uid)
          if (gameSessionModel == null) {
             Ok(Json.obj("status" -> gameSessionDao.lastError))
          }
          if (openGameSession.gameSessionId == "-2") {
             userGameSessionDao.addGameSessionIntoExistingRecord(uid, gameSessionId)
          } else {
             userGameSessionDao.addNewUserGameSession(uid, gameSessionId)
          }
          openGameSessionDao.addNewOpenGameSession(gameSessionId)
          GameSessionUtil.addAnOpenGameSession(new OpenGameSessionModel(gameSessionId))

          Ok(Json.toJson(gameSessionModel))
      } else {
          userGameSessionDao.addNewUserGameSession(uid, openGameSession.gameSessionId)
          val gameSessionModel = gameSessionDao.updateGameSessionOnPlayer(openGameSession.gameSessionId, uid, 1000000000, true)
          Ok(Json.toJson(gameSessionModel))
      }
      
  }

 
}