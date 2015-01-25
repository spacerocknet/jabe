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

  def gameInfo = Action { request =>
    val json: Option[JsValue] = request.body.asJson
    val gameId: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(0)

     val game: Game = gameConfig.getGameInfoByGid(gameId)

     val gameString = Json.obj(
                      "game_name" -> game.gameName,
                      "game_descrption" -> game.gameDescription,
                      "categories" -> Json.toJson(game.categories),
                      "battles_per_game" -> 6
                   )
                   
     Ok(gameString)
  }

  def gameInfoByName = Action { request =>
    val json: Option[JsValue] = request.body.asJson
    val gameName: String = (json.getOrElse(null) \ "game-name").asOpt[String].getOrElse("asteroid")

    val game: Game = gameConfig.getGameInfoByName(gameName)

    val gameString = Json.obj(
      "game_name" -> game.gameName,
      "game_descrption" -> game.gameDescription,
      "categories" -> Json.toJson(game.categories),
      "battles_per_game" -> 6
    )

    Ok(gameString)
  }

   def gameResult = Action { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("NoAuthorization"))

    try {
      val json: Option[JsValue] = request.body.asJson
      println("Body ::: ")
      println(request.body)
      println(json)
      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse("")
      val result = (json.getOrElse(null) \ "result").asOpt[String].getOrElse("0")

      println("userId: " + uid)
      println("result: " + result)
      if (uid != "") {
        val res = result.split("\\\\s+")
        val r = gResult.addGameResults(uid, res.toList)
        if (r)
          Ok("status: \"Ok\"")
        else
          Ok("status: \"Something went wrong\"")

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
}