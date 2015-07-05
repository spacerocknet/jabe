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
import spacerock.constants

import scala.collection.JavaConversions._

class GameSessionController (implicit inj: Injector) extends Controller with Injectable {

  val openGameSessionDao = inject[OpenGameSession]
  val gameSessionDao = inject[GameSession]
  val userGameSessionDao = inject[UserGameSession]
  
  implicit val gameSessionFmt = Json.format[GameSessionModel]
  //implicit val gameSessionMapFmt = Json.format[Map[String, String]]

  /**
   *  Create a new game session or join an existing game session
    * @param uid : user id
   * @return json of a user game session.
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

  def updateGameSession = Action {  request =>
      val json: Option[JsValue] = request.body.asJson
      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gameSessionId = (json.getOrElse(null) \ "game_session_id").asOpt[String].getOrElse("")
      val puzzlePieces = (json.getOrElse(null) \ "puzzle_pieces").asOpt[Int].getOrElse(0)
      val changeTurn = (json.getOrElse(null) \ "change_turn").asOpt[Boolean].getOrElse(false)
      Logger.info("Received uid : " + uid)
      Logger.info("Received gameSessionId : " + gameSessionId)
      Logger.info("Received puzzlePieces : " + puzzlePieces)
      Logger.info("Received changeTurn : " + changeTurn)
      
      var retObj: JsObject = StaticVariables.OkStatus

      try {
         gameSessionDao.updateGameSessionOnPlayer(gameSessionId, uid, puzzlePieces, changeTurn)
      } catch {
        case e: Exception => {
           Logger.error("exception = %s" format e)
           retObj = StaticVariables.BackendErrorStatus
        }
      }
       
      Ok(retObj)
       
  }
  
  def updateGameSessionState = Action { request =>
      val json: Option[JsValue] = request.body.asJson
      val gameSessionId = (json.getOrElse(null) \ "game_session_id").asOpt[String].getOrElse("")
      val gameState = (json.getOrElse(null) \ "state").asOpt[Int].getOrElse(0)
      val gameSessionAttrs = (json.getOrElse(null) \ "attributes").asOpt[JsObject].getOrElse(null)
      
      Logger.info("Received gameSessionId : " + gameSessionId)
      Logger.info("Received gameState : " + gameState)
      Logger.info("Received attributes: " + gameSessionAttrs)
      var map = Map[String, String]()
      if (gameSessionAttrs != null) {
        gameSessionAttrs.fields.foreach( (field) =>  map += (field._1 -> field._2.toString))                                  
      }
      
      //Logger.info("map : " + map.toString())
      var retObj: JsObject = StaticVariables.OkStatus

      try {
        gameSessionDao.updateGameSessionState(gameSessionId, gameState, map)
      } catch {
        case e: Exception => {
           Logger.error("exception = %s" format e)
           retObj = StaticVariables.BackendErrorStatus
        }
      }
       
      Ok(retObj)
  }
  
  def getGameSessionsByUid = Action { request =>
      val json: Option[JsValue] = request.body.asJson
      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      Logger.info("Received uid : " + uid)
      
      var retObj: JsObject = StaticVariables.OkStatus
      try {
         val userGameSessionModel = userGameSessionDao.getUserGameSessionsByUid(uid)
         val gameSessions : List[GameSessionModel] = gameSessionDao.getGameSessionsByIds(userGameSessionModel.gameSessionIds)
         if (gameSessions != null) {
            //retObj = JsArray(Json.toJson(retVal))
           var seq = Seq[JsValue]()
           
           for(gameSession : GameSessionModel <- gameSessions) {
               gameSession.clean()
               /*
               val jsonObj = Json.obj("game_session_id" -> gameSession.gameSessionId,
                                 "state" -> gameSession.state,
                                 "uid_1" -> gameSession.uid1,
                                 "puzzle_pieces_1" -> gameSession.puzzlePieces1,
                                 "uid_1_last_move" -> gameSession.uid1LastMove,
                                 "uid_2" -> gameSession.uid2,
                                 "puzzle_pieces_2" -> gameSession.puzzlePieces2,
                                 "uid_2_last_move" -> gameSession.uid2LastMove,
                                 "current_turn" -> gameSession.currentTurn,
                                 "current_round" -> gameSession.currentRound
                                 )
                                 
                                 */
               val jsonObj = Json.toJson(gameSession)
               //Logger.info("JsonObject: " + jsonObj)
               seq = seq:+ jsonObj
           }

           retObj = Json.obj("game_sessions" -> JsArray(seq))
         }
        
      } catch {
        case e: Exception => {
           Logger.error("exception = %s" format e)
           retObj = StaticVariables.BackendErrorStatus
          
        }
      }
       
       Ok(retObj)
  }
 
}