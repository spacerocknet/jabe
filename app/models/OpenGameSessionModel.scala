package models


case class OpenGameSessionModel (gameSessionId: String) {
  final val fmt =
    """
      |{
      |"game_session_id" : %s
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(gameSessionId)
  }
}
