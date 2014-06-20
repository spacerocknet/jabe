package models

/**
 * Game's result (right and wrong answers)  from a user's game
 *
 * @param userId  - a unique userId
 * @param result - list of Ids of correct questions
 */
case class GameResult(userId: Long, result: String)


