package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/5/15.
 */

object TestAuthCodeController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      var time: Long = System.currentTimeMillis() + 10000000
      println("Valid request auth code")
      testAuthCodeRequest(time, true)
      time = System.currentTimeMillis() - 10000000
      println("Invalid request auth code")
      testAuthCodeRequest(time, false)

      val validToken: String = "nWk8Y6Agx+jD++H2"
      println("Valid request to validate auth code '%s'" format validToken)
      testValidateToken(validToken, true)

      val invalidToken: String = "MlBvpz6pwj0a4TCa"
      println("Invalid request to validate auth code '%s'" format invalidToken)
      testValidateToken(invalidToken, false)

    }
  }

  def testAuthCodeRequest(time: Long, isValid: Boolean): Unit = {

    // valid
    val response = route(FakeRequest(POST, "/v1/auth/request")
      .withJsonBody(Json.obj("expired-time" -> time))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")

    if (isValid)
      contentAsString(response) must contain("auth-code")
    else
      contentAsString(response) must contain("Failed")
  }

  def testValidateToken(token: String, isValid: Boolean): Unit = {
    // invalid
    val response = route(FakeRequest(POST, "/v1/auth/validate")
      .withJsonBody(Json.obj("auth-code" -> token))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
    if (!isValid)
      contentAsString(response) must contain("Failed")
    else
      contentAsString(response) must contain("OK")
  }
}
