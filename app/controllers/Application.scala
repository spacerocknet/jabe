package controllers

import java.util.UUID

import play.api.mvc._
import scaldi.{Injectable, Injector}
import spacerock.persistence.{NewQuiz, NewCategory, NewUserData}
import play.api.mvc.Controller

class Application(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [NewUserData]
  val category = inject[NewCategory]
  val quiz = inject[NewQuiz]

  def index = Action {
     Ok(views.html.index("Test User"))
  }

//  def getAllQuiz = Action {
//    println(quiz.getAllQuizzes().toString())
//    Ok(quiz.getAllQuizzes().toString())
//  }
//
//  def getQuizById = Action { request =>
//    try {
//      val json: Option[JsValue] = request.body.asJson
//      println(json)
//      val qid = (json.orNull(null) \ "qid").asOpt[Int].getOrElse(-1)
//      if (qid >= 0) {
//        val qa: QuAn = quiz.getQuizByQid(qid)
//        print(qa.toString)
//        Ok(qa.toString)
//      }
//      else {
//        BadRequest("Error")
//      }
//    } catch {
//      case e: Exception => {
//        e.printStackTrace()
//        BadRequest("Invalid EAN")
//      }
//    }
//  }
//
//  def getQuizByCategory = Action { request =>
//    try {
//      val json: Option[JsValue] = request.body.asJson
//      println(json)
//      val category = (json.orNull(null) \ "category").asOpt[String].orNull(null)
//      if (category != null) {
//        val l: List[QuAn] = quiz.getQuizzesByCategory(category)
//        print(l.toString)
//        Ok(l.toString)
//      }
//      else {
//        BadRequest("Error")
//      }
//    } catch {
//      case e: Exception => {
//        e.printStackTrace()
//        BadRequest("Invalid EAN")
//      }
//    }
//  }
//
//  def addNewQuiz = Action { request =>
//    try {
//      val json: Option[JsValue] = request.body.asJson
//      println(json)
//      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1).asInstanceOf[Long].toLong
//      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
//      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse(null)
//      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].getOrElse(null)
//      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
//      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse(null)
//      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse(null)
//      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse(null)
//
//      if ((category != null) && (qid >= 0)) {
//        val res: Boolean = quiz.addNewQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
//        Ok("")
//      } else {
//        BadRequest("Error")
//      }
//    } catch {
//      case e: Exception => {
//        e.printStackTrace()
//        BadRequest("Invalid EAN")
//      }
//    }
//  }
//
//  def updateQuiz = Action { request =>
//    try {
//      val json: Option[JsValue] = request.body.asJson
//      println(json)
//      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1).asInstanceOf[Long].toLong
//      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse(null)
//      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse(null)
//      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].getOrElse(null)
//      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
//      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse(null)
//      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse(null)
//      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse(null)
//
//      if ((category != null) && (qid >= 0)) {
//        val res: Boolean = quiz.updateQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
//        Ok("")
//      } else {
//        BadRequest("Error")
//      }
//    } catch {
//      case e: Exception => {
//        e.printStackTrace()
//        BadRequest("Invalid EAN")
//      }
//    }
//  }
}