package controllers

import models.{GameResultModel, GameModel}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence.cassandra.{GameResult, GameInfo}

class GameController (implicit inj: Injector) extends Controller with Injectable {
  val gInfo = inject[GameInfo]
  val gResult = inject[GameResult]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")

  /**
   * Get game info by game id. This method use GET request only
    * @param gameId game id
   * @return game info if success, otherwise empty json string.
   */
  def getGameInfo (gameId: Int) = Action {
     val game: GameModel = gInfo.getGameInfoByGid(gameId)
     if (game != null) {
       val gameString = Json.obj(
                        "game-name" -> game.gameName,
                        "game-description" -> game.gameDescription,
                        "categories" -> Json.toJson(game.categories),
                        "battles-per-game" -> game.bpg
                     )
       Ok(gameString)
    } else {
       Ok("{}")
     }
  }

  /**
   * Get game info by game name.
   * @param gameName game name
   * @return game info if success, otherwise empty json object.
   */
  def getGameInfoByName (gameName : String) = Action {

    val game: GameModel = gInfo.getGameInfoByName(gameName)
    if (game != null) {
      val gameString = Json.obj(
        "game-name" -> game.gameName,
        "game-description" -> game.gameDescription,
        "categories" -> Json.toJson(game.categories),
        "battles-per-game" -> game.bpg
      )
      Ok(gameString)
    } else {
      Ok("{}")
    }


  }

  /**
   * Add new game result. Json body contains; uid, game-id, level, score
   * @return Ok status if success, otherwise failed status, service unavailable, bad request
   */
  def addNewGameResult = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gid: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(-1)
      val level: Int = (json.getOrElse(null) \ "level").asOpt[Int].getOrElse(-1)
      val score: Long = (json.getOrElse(null) \ "score").asOpt[Long].getOrElse(-1)

      if (uid != "" && gid > 0) {

        val r = gResult.addResults(uid, gid, level, score)
        if (r)
          Ok(OkStatus)
        else
          ServiceUnavailable("Service is currently unavailable")
      }
      BadRequest(FailedStatus)
    }
    catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Get game result by user id of all time and in all game. This method serve GET request
   * @param uid user id
   * @return list of game result models (uid, game id, level, score) if success, otherwise empty json object
   */
  def getGameResult (uid : String) = Action {

    val res: List[GameResultModel] = gResult.getResultsByUid(uid)
    if (uid != null) {
      if (res == null) {
        ServiceUnavailable("Service is currently unavailable")
      }
      val gameString = Json.obj(
        "results" -> Json.toJson(res)
      )
      Ok(gameString)
    } else {
      Ok("{}")
    }
  }

  /**
   * Get game result. This method serve POST method. Json body contains: uid, game-id.
   * This method is for getting score of all level in @game-id that @uid played.
   * @return map of level-score if success, otherwise Service unavailable, failed status or bad request
   */
  def getGameResult () = Action { request =>

    try {
      val json: Option[JsValue] = request.body.asJson

      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gid: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(-1)

      if (uid != "" && gid > 0) {
        val r: Map[Int, Long] = gResult.getResultsByUidOfGame(uid, gid)

        if (r != null)
          Ok(Json.toJson(r))
        else
          ServiceUnavailable("Service is currently unavailable")
      }
      Ok(FailedStatus)
    }
    catch {
      case e: Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }


}