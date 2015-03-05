package spacerock.constants

/**
 * Created by william on 2/25/15.
 */
object Constants {
  val REDIS_UID_KEY: String = "uid"
  val REDIS_CLIENT_KEY: String = "client-id"
  val REDIS_BLOCK_ID_KEY: String = "block-id"
  val REDIS_QUIZ_ID_KEY: String = "quiz-id"
  val REDIS_SKY_ID_KEY: String = "sku-id"

  //
  val MAX_UID_BLOCK_SIZE: Int = 1000

  val MAX_LOCK_TRIES: Int = 10

  val AUTH_CODE_LENGTH: Int = 12
}
