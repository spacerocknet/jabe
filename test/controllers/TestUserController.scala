package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

/**
 * Created by william on 3/5/15.
 */

object TestUserController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val os = "os 1"
      val platform = "platform 1"
      val phone = "phone 1"
      val model = "model 1"
      val deviceuuid = "uuid 1"
      val username = "uname 1"
      val fname = "first name 1"
      val lname = "last name 1"
      val email = "email 1"
      val fbid = "fb id 1"
      val state = "state 1"
      val region = "region 1"
      val country = "country 1"
      val apps = "apps 1"
      println("insert")
      val listUid: ListBuffer[String] = new ListBuffer[String]
      for (i <- 0 to 10) {
        listUid.add(testAddNoInfo(os + i, platform + i, phone + i, model + i, deviceuuid + i))
      }
      println("update")
      // update
      for (i <- 0 until listUid.size) {
        testUpdateInfo(listUid.get(i), username + i, fname + i, lname + i, email + i,
                       fbid + i, state + i, region + i, country + i, apps + i)
      }

      testGetByUname(username + 0)
      testGetByUname(username + 100)

      testGetByUid("afjkdsajfldksafdsa")
      testGetByUid(listUid.get(0))

      testGetAllUser()

    }
  }

  def testAddNoInfo(os: String, platform: String, phone: String,
                     model: String, deviceUuid: String): String = {
    val response = route(FakeRequest(POST, "/v1/user/addnoinfo")
      .withJsonBody(Json.obj("os" -> os,
      "platform" -> platform,
      "phone" -> phone,
      "model" -> model,
      "device-uuid" -> deviceUuid))).get
    println(contentAsString(response))
    val uid: String = (contentAsJson(response) \ "uid").asOpt[String].getOrElse("")
    uid
  }

  def testUpdateInfo(uid: String, username: String, firstName: String,
                      lastName: String, email: String, fbId: String,
                      state: String, region: String, country: String, apps: String): Unit = {
    val response = route(FakeRequest(POST, "/v1/user/updateinfo")
      .withJsonBody(Json.obj("uid" -> uid,
                            "user-name" -> username,
                            "first-name" -> firstName,
                            "last-name" -> lastName,
                            "email" -> email,
                            "fb-id" -> fbId,
                            "state" -> state,
                            "region" -> region,
                            "country" -> country,
                            "apps" -> apps))).get
    println(contentAsString(response))
  }

  def testGetByUname(username: String): Unit = {
    val response = route(FakeRequest(POST, "/v1/user/byname")
    .withJsonBody(Json.obj("user-name" -> username))).get
    println(contentAsString(response))
  }

  def testGetByUid(uid: String): Unit = {
    val response = route(FakeRequest(GET, "/v1/user/byuid/%s" format uid)).get
    println(contentAsString(response))
  }

  def testGetAllUser(): Unit = {
    val response = route(FakeRequest(GET, "/v1/user/all")).get
    println(contentAsString(response))
  }
}


