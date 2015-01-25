package models


import scala.collection.JavaConversions._


case class Subscriber(val platform: String, val os: String, val model: String, 
                      val phone: String, val deviceUuid: String) {
    
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