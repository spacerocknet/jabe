package controllers

import spacerock.persistence.{NewQuiz, NewCategory, NewUserData}

import scala.collection.JavaConversions._

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Controller
import scaldi.Injectable
import scaldi.Injector

import scala.util.Random

import models._

class NewQuizController(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [NewUserData]
  val category = inject[NewCategory]
  val quiz = inject[NewQuiz]
  val OkStatus = Json.obj("status" -> "OK")

  def quizRequest = Action { request =>

     try {
       val json: Option[JsValue] = request.body.asJson
       val cat = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("Movies")
       val num = (json.getOrElse(null) \ "num").asOpt[Int].getOrElse(1)

       // check if the requested category does not exist
//       if (category.getCategoryByName(cat) == null) {
//         Ok(JsArray())
//       } else {
       val r: Random = new Random()
         var seq = Seq[JsObject]()
         val questions: List[QuAn] = quiz.getQuizzesByCategory(cat)
       if (questions != null) {
         for (i <- 0 until num) {
           if (questions.size > 0) {
             val index = Math.abs(r.nextInt()) % questions.size
             val q = questions.get(index)
             println(q)
             val jsonObj = Json.obj("category" -> q.category,
               "qid" -> q.qid,
               "question" -> q.question,
               "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
               "df" -> JsNumber(q.df)
             )
             println(jsonObj)
             seq = seq :+ jsonObj
           }
         }
       }
         Ok(JsArray(seq))

//       }
    } catch {
      case e:Exception => {
        e.printStackTrace()
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  def getAllQuiz = Action {
    try {
      var seq = Seq[JsObject]()
      val questions: List[QuAn] = quiz.getAllQuizzes()

      for(q <- questions) {
        val jsonObj = Json.obj("category" -> q.category,
                               "qid" -> q.qid,
                               "question" -> q.question,
                               "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
                               "df" -> JsNumber(q.df)
                              )
        seq = seq :+ jsonObj
      }

      Ok(JsArray(seq))
    } catch {
      case e: Exception => {
        e.printStackTrace
        BadRequest("Invalid EAN")
      }
    }
  }

  def getQuizById (qid: Long) = Action {
    try {
      if (qid >= 0) {
        val q: QuAn = quiz.getQuizByQid(qid)
        if (q != null) {
          val jsonObj = Json.obj("category" -> q.category,
            "qid" -> q.qid,
            "question" -> q.question,
            "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
            "df" -> JsNumber(q.df)
          )
          Ok(jsonObj)
        } else {
          Ok("{}")
        }
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

  def getQuizByCategory (catName: String) = Action {
    try {
      if (catName != null) {
        var seq = Seq[JsObject]()
        val l: List[QuAn] = quiz.getQuizzesByCategory(catName)
        for(q <- l) {
          val jsonObj = Json.obj("category" -> q.category,
                                 "qid" -> q.qid,
                                 "question" -> q.question,
                                 "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
                                 "df" -> JsNumber(q.df)
                                )
          seq = seq:+ jsonObj
        }

        Ok(JsArray(seq))
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
      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1)
      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("")
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse("")
      val rightAns = (json.getOrElse(null) \ "right-answer").asOpt[String].getOrElse("")
      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse("")
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse("")
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse("")

      if ((category != null) && (qid >= 0)) {
        val res: Boolean = quiz.addNewQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
        if (res)
          Ok("Insert quiz successful")
        else
          Ok("Cannot insert quiz to database")
      } else {
        BadRequest("Cannot insert new quiz to database")
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
      val qid: Long = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(-1)
      val category = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val question = (json.getOrElse(null) \ "question").asOpt[String].orNull(null)
      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].orNull(null)
      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].orNull(null)
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].orNull(null)
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].orNull(null)

      if ((category != null) && (qid >= 0)) {
        val res: Boolean = quiz.updateQuiz(qid, category, question, rightAns, ans1, ans2, ans3, df)
        if (res)
          Ok("Update quiz successful")
        else
          Ok("Cannot update quiz")
      } else {
        BadRequest("Cannot update quiz %s" format json)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }
}
