package models

import play.api.libs.json.Json

/**
 * Created by william on 2/24/15.
 */
case class GameModel(gameId: Int, gameName: String, gameDescription: String,
                     categories: Set[String], bpg: Int) {
  final val fmt: String =
    """
      |{
      |"game-id" : %d,
      |"game-name" : "%s",
      |"description" : "%s",
      |"categories" : %s,
      |"battle-per-game" : %d
      |}
    """.stripMargin
  override def toString(): String = {
    fmt.format(gameId, gameName, gameDescription, Json.toJson(categories), bpg)
  }
}
