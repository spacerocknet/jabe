package controllers

import models.Game
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence._

class GameController (implicit inj: Injector) extends Controller with Injectable {
  val gameConfig = inject[GameConfig]
  val gResult = inject[GameResult]

  def gameInfo (gameId: Int) = Action {
    printf("ID: " + gameId)
     val game: Game = gameConfig.getGameInfoByGid(gameId)
     if (game != null) {
       val gameString = Json.obj(
                        "game_name" -> game.gameName,
                        "game_description" -> game.gameDescription,
                        "categories" -> Json.toJson(game.categories),
                        "battles_per_game" -> game.bpg
                     )
                     
       Ok(gameString)
    } else {
       Ok("{}")
     }
  }

  def gameInfoByName (gameName : String) = Action {

    val game: Game = gameConfig.getGameInfoByName(gameName)
    println(gameName)
    if (game != null) {
      val gameString = Json.obj(
        "game_name" -> game.gameName,
        "game_description" -> game.gameDescription,
        "categories" -> Json.toJson(game.categories),
        "battles_per_game" -> game.bpg
      )
      Ok(gameString)
    } else {
      Ok("{}")
    }


  }

  // overwrite result list
  def gameNewResult = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val results = (json.getOrElse(null) \ "results").asOpt[String].getOrElse("")

      if (uid != "") {
        val res = results.split("\\s+")
        val r = gResult.addGameResults(uid, res.toList)
        if (r)
          Ok("status: \"Ok\"")
        else
          ServiceUnavailable("Service is currently unavailable")
      }
      Ok("Ok")
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  // add to result list, not overwrite
  def gameResult = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val results = (json.getOrElse(null) \ "results").asOpt[String].getOrElse("")

      if (uid != "") {
        val res = results.split("\\s+")
        println(res.length)
        val r = gResult.addMoreGameResults(uid, res.toList)
        if (r)
          Ok("status: \"Ok\"")
        else {
            ServiceUnavailable("Service is currently unavailable")
        }

      }
      Ok("Ok")
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  def getResult (uid : String) = Action {

    val res: List[String] = gResult.getGameResults(uid)
    println(uid)
    if (uid != null) {
      if (res == null) {
        ServiceUnavailable("Service is currently unavailable")
      }
      val sb: StringBuilder = new StringBuilder
      for (i <- 0 until res.length - 2) {
        println(res(i))
        sb.append(res(i))
      }
      sb.append(res(res.length - 1))
      val gameString = Json.obj(
        "results" -> sb.toString()
      )
      Ok(gameString)
    } else {
      Ok("{}")
    }
  }

}