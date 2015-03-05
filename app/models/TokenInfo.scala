package models

import java.util.Date

/**
 * Created by william on 3/4/15.
 */
case class TokenInfo (code: String, createdTime: Date, expiredTime: Date, status: Boolean) {
  final val fmt =
    """
      |{
      |"code" : "%s",
      |"created-time" : "%s",
      |"expired-time" : "%s",
      |"status" : %b
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(code, createdTime.toString, expiredTime.toString, status)
  }
}
