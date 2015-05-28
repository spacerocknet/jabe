package models

import play.api.libs.json.Json


case class UserGameSessionModel (uid: String, gameSessionIds: List[String]) {
  final val fmt =
    """
      |"{
      |uid" : "%s",
      |"game_session_ids" : %s"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(uid, Json.toJson(gameSessionIds))
  }
}