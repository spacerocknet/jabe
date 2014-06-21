package models


import scala.collection.JavaConversions._


case class Subscriber(val uuid : String, val platform: String, val os: String, val model: String, 
                      val phone: String, val deviceUuid: String) {
    
    //var firstName : String
    //var lastName : String
    //var userName : String
    
    //var email : String
    //var fbId : String //FaceBook ID
    
    //var state : String //state like california
    //var region : String //some country does not have state but region
    //var country: String
    
    //var apps : String   //Asteroid
    //var registeredTime : Long
    //var lastSeen : Long
    
    
}