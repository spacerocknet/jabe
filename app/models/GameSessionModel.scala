package models


case class GameSessionModel (gameSessionId: String, var state: Int, 
                             var uid1: String, var puzzlePieces1: Int, var uid1LastMove: Long,
                             var uid2: String, var puzzlePieces2: Int, var uid2LastMove: Long,
                             var currentTurn: Int, var currentRound: Int) {
  final val fmt =
    """
      |{
      |"game_session_id" : %s,
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
    fmt.format(gameSessionId, state, 
               uid1, puzzlePieces1, uid1LastMove, 
               uid2, puzzlePieces2, uid2LastMove, 
               currentTurn, currentRound)
  }
  
  def clean() = {
     uid2 = if (uid2 == null) "null" else uid2
  }
}
