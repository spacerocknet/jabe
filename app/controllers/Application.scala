package controllers

import java.util.UUID

import play.api.mvc._
import scaldi.{Injectable, Injector}
import spacerock.persistence.{NewQuiz, NewCategory, NewUserData, DemoDJD}
import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Controller

class Application(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [NewUserData]
  val category = inject[NewCategory]
  val quiz = inject[NewQuiz]

  def index = Action {
     Ok(views.html.index("Test User"))
  }

  def getAllUser = Action {
    val s: List[Subscriber] = userDao.getAllUsers()
    println(s)
    Ok(s.toString)
  }

  def getUserInfoByUID = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val uidString = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse(null)
      if (uidString != null) {
        val uid: UUID = UUID.fromString(uidString)

        Ok(userDao.getUserInfoByUID(uid).toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e: Exception => {
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }

  }

  def getUserInfoByUsername = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val userName = (json.getOrElse(null) \ "username").asOpt[String].getOrElse(null)
      if (userName != null) {

        Ok(userDao.getUserInfoByUsername(userName).toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e: Exception => {
        println("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }

  }

  def addNewUser = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val userName = (json.getOrElse(null) \ "username").asOpt[String].getOrElse(null)
      val firstName = (json.getOrElse(null) \ "firstname").asOpt[String].getOrElse("")
      val lastName = (json.getOrElse(null) \ "lastname").asOpt[String].getOrElse("")
      val email = (json.getOrElse(null) \ "email").asOpt[String].getOrElse("")
      val fbId = (json.getOrElse(null) \ "fbid").asOpt[String].getOrElse("")
      val locState = (json.getOrElse(null) \ "locstate").asOpt[String].getOrElse("")
      val locRegion = (json.getOrElse(null) \ "locregion").asOpt[String].getOrElse("")
      val appName = (json.getOrElse(null) \ "appname").asOpt[String].getOrElse("")
      if (userName != null) {
        //generate uuid
        val uid: UUID = UUID.randomUUID()

        userDao.addUserInfo(uid, userName, firstName, lastName, email, fbId,
                            locState, locRegion, appName)
        Ok("\"uid\":\"" + uid.toString + "\"")
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def getAllQuiz = Action {
    println(quiz.getAllQuizzes().toString())
    Ok(quiz.getAllQuizzes().toString())
  }

  def getQuizById = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val qid = (json.getOrElse(null) \ "qid").asOpt[Int].getOrElse(-1)
      if (qid >= 0) {
        val qa: QuAn = quiz.getQuizByQid(qid)
        print(qa.toString)
        Ok(qa.toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def getQuizByCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
      if (category != null) {
        val l: List[QuAn] = quiz.getQuizzesByCategory(category)
        print(l.toString)
        Ok(l.toString)
      }
      else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def addNewQuiz = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1).asInstanceOf[Long].toLong
      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse(null)
      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].getOrElse(null)
      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse(null)
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse(null)
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse(null)

      if ((category != null) && (qid >= 0)) {
        val res: Boolean = quiz.addNewQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
        Ok("")
      } else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def updateQuiz = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1).asInstanceOf[Long].toLong
      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse(null)
      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].getOrElse(null)
      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse(null)
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse(null)
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse(null)

      if ((category != null) && (qid >= 0)) {
        val res: Boolean = quiz.updateQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
        Ok("")
      } else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }


  def getAllCategory = Action {
    println(category.getAllCategories().toString())
    Ok(category.getAllCategories().toString())
  }

  def getCategoryByName = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)

      if (catName != null) {
        val cat: Category = category.getCategoryByName(catName)
        Ok(cat.toString)
      } else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def updateCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
      val bpg = (json.getOrElse(null) \ "battles_per_game").asOpt[Int].getOrElse(0)

      if (category != null) {
        if (category.updateCategory(catName, bpg))
          Ok("")
        else
          Ok("error")
      } else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  def addCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
      val bpg = (json.getOrElse(null) \ "battles_per_game").asOpt[Int].getOrElse(0)

      if (category != null) {
        if (category.addNewCategory(catName, bpg))
          Ok("")
        else
          Ok("error")
      } else {
        BadRequest("Error")
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

}