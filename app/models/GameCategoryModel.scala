package models

import play.api.libs.json.Json

/**
 * Created by william on 3/20/15.
 */
case class GameCategoryModel (gameId: Int, description: String, categories: Set[String]) {
  final val fmt: String = new String(
    """{
      |"game_id" : %d,
      |"description" : "%s",
      |"categories" : %s
      |}""".stripMargin)

  override def toString(): String = {
    fmt.format(gameId, description, Json.toJson(categories))
  }
}
