package spacerock.constants

/**
 * Created by william on 2/25/15.
 */
object Constants {
  val LOCK_UID_KEY: String = "uid"
  val REDIS_CLIENT_KEY: String = "client-id"
  val REDIS_BLOCK_ID_KEY: String = "block-id"
  val REDIS_QUIZ_ID_KEY: String = "quiz-id"
  val REDIS_SKU_ID_KEY: String = "sku-id"
  val REDIS_GAME_ID_KEY: String = "game-id"

  //
  val MAX_UID_BLOCK_SIZE: Int = 1000

  val MAX_LOCK_TRIES: Int = 10

  val AUTH_CODE_LENGTH: Int = 10

  val DEFAULT_RESULT_SIZE = 5

  // error code constants
  object ErrorCode {
    val ERROR_SUCCESS: Int = 0

    object CassandraDb {
      val ERROR_CAS_NO_HOST_AVAILABLE: Int   = 10000
      val ERROR_CAS_QUERY_EXECUTION: Int     = 10001
      val ERROR_CAS_QUERY_VALIDATION: Int    = 10002
      val ERROR_CAS_UNSUPPORTED_FEATURE: Int = 10003
      val ERROR_CAS_NOT_INITIALIZED: Int     = 10004
      val ERROR_CAS_AUTHENTICATION: Int      = 10005
      val ERROR_CAS_ILLEGAL_STATE: Int       = 10006
    }

    object DataError {
      val DATA_EXISTED: Int  = 20001
    }

  }
}
