package models


case class GameSessionModel (gameSessionId: String, state: Int, 
                             uid1: String, puzzlePieces1: Int, uid1LastMove: BigInt,
                             uid2: String, puzzlePieces2: Int, uid2LastMove: BigInt,
                             currentTurn: Int, currentRound: Int) {
  final val fmt =
    """
      |{
      |"game_session_id" : %d,
      |"state" : "%s",
      |"uid_1" : "%s",
      |"puzzle_pieces_1" : %d,
      |"uid_1_last_move" : %d,
      |"uid_2" : "%s",
      |"puzzle_pieces_2" : %d,
      |"uid_2_last_move" : %d,
      |"current_turn" : %d,
      |"current_round": %d
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(gameSessionId, state, uid1, puzzlePieces1, uid1LastMove, uid2, puzzlePieces2, uid2LastMove, currentTurn, currentRound)
  }
}
