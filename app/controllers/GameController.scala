package controllers

import models.{GameResultModel, GameModel}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import spacerock.persistence.cassandra.{Category, GameResult, GameInfo}
import spacerock.utils.{StaticVariables, IdGenerator}

class GameController (implicit inj: Injector) extends Controller with Injectable {
  val gInfo = inject[GameInfo]
  val gResult = inject[GameResult]
  val category = inject[Category]
  val idGenerator = inject[IdGenerator]
  implicit val gameResultFmt = Json.format[GameResultModel]

  /**
   * Add new game to database.
   * @return
   */
  def addNewGameInfo = Action { request =>
    var retObj: JsObject = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val gameName: String = (json.getOrElse(null) \ "game_name").asOpt[String].getOrElse("")
      val description: String = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")
      val categories = (json.getOrElse(null) \ "categories").as[JsArray].as[List[String]].toSet
      val bpg: Int = (json.getOrElse(null) \ "battles_per_game").asOpt[Int].getOrElse(-1)
      val gameId: Int = idGenerator.generateNextId(Constants.REDIS_GAME_ID_KEY).toInt

      if (gInfo.addGameInfo(gameId, gameName, description, categories, bpg)) {
        var isSuccess: Boolean = true
        categories.foreach(cat => {
          if (!category.addNewGame(cat, gameId))
            isSuccess = false
        })

        if (!isSuccess) {
          Logger.error("Insert game success, but cannot update game list in category. %s" format json.toString)
          retObj = StaticVariables.DbErrorStatus
        } else {
          retObj = Json.obj("game_id" -> gameId)
        }
      } else {
        Logger.warn("Cannot add new game to database. Please check database again. %s" format json.toString)
        retObj = StaticVariables.DbErrorStatus
      }
    }
    catch {
      case e:Exception => {
        e.printStackTrace()
        Logger.info("exception = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }
  /**
   * Get game info by game id. This method use GET request only
    * @param gameId game id
   * @return game info if success, otherwise empty json string.
   */
  def getGameInfo (gameId: Int) = Action {
     val game: GameModel = gInfo.getGameInfoByGid(gameId)
     if (game != null) {
       val gameString = Json.obj(
                        "game_id" -> game.gameId,
                        "game_name" -> game.gameName,
                        "description" -> game.gameDescription,
                        "categories" -> Json.toJson(game.categories),
                        "battles_per_game" -> game.bpg
                     )
       Ok(gameString)
    } else {
       if (gInfo.lastError == Constants.ErrorCode.ERROR_SUCCESS)
        Ok(Json.obj())
       else
         Ok(Json.obj("status" -> gInfo.lastError))
     }
  }

  /**
   * Get game info by game name.
   * @return game info if success, otherwise empty json object.
   */
  def getGameInfoByName = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val gameName: String = (json.getOrElse(null) \ "game_name").asOpt[String].getOrElse("")

      val game: GameModel = gInfo.getGameInfoByName(gameName)
      if (game != null) {
        val gameString = Json.obj(
          "game_id" -> game.gameId,
          "game_name" -> game.gameName,
          "description" -> game.gameDescription,
          "categories" -> Json.toJson(game.categories),
          "battles_per_game" -> game.bpg
        )
        Ok(gameString)
      } else {
        if (gInfo.lastError == Constants.ErrorCode.ERROR_SUCCESS)
          Ok(Json.obj())
        else
          Ok(Json.obj("status" -> gInfo.lastError))
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }

  }

  /**
   * Add new game result. Json body contains; uid, game-id, level, score
   * @return Ok status if success, otherwise failed status, service unavailable, bad request
   */
  def addNewGameResult = Action { request =>
    var retObj: JsValue = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gid: Int = (json.getOrElse(null) \ "game_id").asOpt[Int].getOrElse(-1)
      val level: Int = (json.getOrElse(null) \ "level").asOpt[Int].getOrElse(-1)
      val score: Long = (json.getOrElse(null) \ "score").asOpt[Long].getOrElse(-1)

      if (uid != "" && gid > 0) {

        val r = gResult.addResults(uid, gid, level, score)
        if (r)
          retObj = StaticVariables.OkStatus
        else {
          Logger.warn("Cannot insert new result to database. Please check database again. %s" format json.toString)
          if (gResult.lastError == Constants.ErrorCode.ERROR_SUCCESS)
            Ok(StaticVariables.OkStatus)
          else
            Ok(Json.obj("status" -> gResult.lastError))
        }
      } else {
        Logger.warn("Bad request. %s" format json.toString)
        Ok(StaticVariables.InputErrorStatus)
      }
      Ok(retObj)
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Get game result by user id of all time and in all game. This method serve GET request
   * @param uid user id
   * @return list of game result models (uid, game id, level, score) if success, otherwise empty json object
   */
  def getGameResultByUid (uid : String) = Action {

    val res: List[GameResultModel] = gResult.getResultsByUid(uid)
    if (uid != null) {
      if (res == null) {
        ServiceUnavailable("Service is currently unavailable")
      }
      Ok(Json.toJson(res))
    } else {
      if (gResult.lastError == Constants.ErrorCode.ERROR_SUCCESS)
        Ok(Json.obj())
      else
        Ok(Json.obj("status" -> gResult.lastError))
    }
  }

  /**
   * Get game result. This method serve POST method. Json body contains: uid, game-id.
   * This method is for getting score of all level in @game-id that @uid played.
   * @return map of level-score if success, otherwise Service unavailable, failed status or bad request
   */
  def getGameResultByUidGame () = Action { request =>

    try {
      val json: Option[JsValue] = request.body.asJson

      val uid: String = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val gid: Int = (json.getOrElse(null) \ "game_id").asOpt[Int].getOrElse(-1)

      if (uid != "" && gid > 0) {
        val r: Map[Int, Long] = gResult.getResultsByUidOfGame(uid, gid)

        if (r != null) {
          val sb: StringBuilder = new StringBuilder
          var count: Int = 0
          val size: Int = r.size
          sb.append("[")
          r.foreach(kv => {
            sb.append("""{"level": %d, "score": %d}""".format(kv._1, kv._2))
            count = count + 1
            if (count < size) {
              sb.append(",")
            }
          })
          sb.append("]")
          Ok(sb.toString)
        } else {
          if (gResult.lastError == Constants.ErrorCode.ERROR_SUCCESS)
            Ok(Json.arr())
          else
            Ok(Json.obj("status" -> gResult.lastError))
        }
      } else {
        Logger.warn("Bad request. %s" format json.toString)
        Ok(StaticVariables.InputErrorStatus)
      }
    }
    catch {
      case e: Exception => {
        Logger.info("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }
}