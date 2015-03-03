package models

import java.util.Date

/**
 * Created by william on 2/24/15.{
    fmt.format(uid, firstName, lastName, userName, email, fbId, state, region,
    apps, registeredTime, lastSeen, platform, os, model, phone, deviceUuid, Json.toJson(deviceSet))
  }

 */
case class DeviceModel(dUuid: String, rt: Date, uid: String = "", os: String = "",
                       platform: String = "", model: String = "", phone: String = "") {
  final val fmt =
    """
      |{
      |"device-uuid" : "%s",
      |"registered-time" : "%s",
      |"uid" : "%s",
      |"os" : "%s",
      |"platform" : "%s",
      |"model" : "%s",
      |"phone" : "%s"
      |}
    """.stripMargin
  override def toString(): String = {
    fmt.format(dUuid, rt.toString, uid, os, platform, model, phone)
  }
}
