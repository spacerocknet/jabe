package models

import play.api.libs.json.Json

case class SubscriberModel(platform: String, os: String, model: String,
                      phone: String, deviceUuid: String) {

  final val fmt: String =
    """
      |{
      |"uid" : "%s",
      |"first-name" : "%s",
      |"last-name" : "%s",
      |"username" : "%s",
      |"email" : "%s",
      |"fbid" : "%s",
      |"state" : "%s",
      |"region" : "%s",
      |"apps" : "%s",
      |"registered-time" : %d,
      |"last-seen" : %d,
      |"platform" : "%s",
      |"os" : "%s",
      |"model" : "%s",
      |"phone" : "%s",
      |"device-uuid" : "%s",
      |"device-set" : %s
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(uid, firstName, lastName, userName, email, fbId, state, region,
    apps, registeredTime, lastSeen, platform, os, model, phone, deviceUuid, Json.toJson(deviceSet))
  }

  var uid: String = ""
  var firstName : String = ""
  var lastName : String = ""
  var userName : String = ""

  var email : String = ""
  var fbId : String = "" //FaceBook ID

  var state : String = ""//state like california
  var region : String = ""//some country does not have state but region
  var country: String = ""

  var apps : String = ""  //Asteroid
  var registeredTime : Long = 0L
  var lastSeen : Long = 0L

  var deviceSet: Set[String] = null

  def this(uid: String,
           platform: String, os: String, model: String,
           phone: String, deviceUuid: String) = {
     this(platform, os, model, phone, deviceUuid)
     this.uid = uid
     this.apps = apps
     this.lastSeen = System.currentTimeMillis()
  }

  def this(uid: String,
           platform: String, os: String, model: String,
           phone: String, deviceUuid: String,
           email: String, fbId: String,
           state: String, region: String, country: String,
           apps: String) = {
     this(platform, os, model, phone, deviceUuid)
     this.uid = uid
     this.email = email
     this.fbId = fbId
     this.state = state
     this.region = region
     this.country = country
     this.apps = apps
     this.lastSeen = System.currentTimeMillis()
  }
    
}