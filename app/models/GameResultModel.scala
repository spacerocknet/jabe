package models

/**
 * Game's result (right and wrong answers)  from a user's game
 *
 * @param gid  a unique game
 * @param level level of the corresponding game
 * @param score score of level in game that user played
 * @param uid User id
 */
case class GameResultModel(gid: Int, level: Int, score: Long, uid: String) {
  final val fmt: String =
    """
      |{
      |"game-id" : %d,
      |"level" : %d,
      |"score" : %d,
      |"uid" : "%s"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(gid, level, score, uid)
  }
}


