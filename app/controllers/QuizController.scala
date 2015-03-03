package controllers

import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import spacerock.persistence.cassandra.{Category, Quiz, UserData}
import spacerock.utils.IdGenerator

import scala.collection.JavaConversions._
import scala.util.Random

class QuizController(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [UserData]
  val category = inject[Category]
  val quiz = inject[Quiz]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")
  val idGenerator = inject [IdGenerator]

  /**
   * Request a number of quizzes by category. json body contains: category and number of requested quiz.
   * At first, it will check category to see if the category existed in system or not.
   * @return array of quizzes if success, otherwise bad request.
   */
  def quizRequest = Action { request =>

     try {
       val json: Option[JsValue] = request.body.asJson
       val cat = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("Movies")
       val num = (json.getOrElse(null) \ "num").asOpt[Int].getOrElse(1)

       // check if the requested category does not exist
       if (category.getCategoryByName(cat) == null) {
         Ok(JsArray())
       } else {
         val r: Random = new Random()
         var seq = Seq[JsObject]()
         val questions: List[QuAnModel] = quiz.getQuizzesByCategory(cat)
         if (questions != null) {
           for (i <- 0 until num) {
             if (questions.size > 0) {
               val index = Math.abs(r.nextInt()) % questions.size
               val q = questions.get(index)
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
       }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Get all quiz in system.
   * @return array of quizzes if success, otherwise bad request
   */
  def getAllQuiz = Action {
    try {
      var seq = Seq[JsObject]()
      val questions: List[QuAnModel] = quiz.getAllQuizzes()

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
        Logger.error("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Get quiz by quiz's id
   * @param qid quiz's id
   * @return json object if success, otherwise empty json, bad request
   */
  def getQuizById (qid: Long) = Action {
    try {
      if (qid >= 0) {
        val q: QuAnModel = quiz.getQuizByQid(qid)
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
        Logger.error("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Get quizzes by category
   * @param catName
   * @return array of quizzes if success, otherwise Bad request
   */
  def getQuizByCategory (catName: String) = Action {
    try {
      if (catName != null) {
        var seq = Seq[JsObject]()
        val l: List[QuAnModel] = quiz.getQuizzesByCategory(catName)
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
        BadRequest(FailedStatus)
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Add new quiz to system. Json body contains: category, question, right answer, df, answer 1, answer 2, answer 3.
   * Quiz's id is generated from IdGenerator by using redis.
   * @return quiz's id if success, otherwise Bad request, failed status
   */
  def addNewQuiz = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      println(json)
      val qid: Long = idGenerator.generateNextId(Constants.REDIS_QUIZ_ID_KEY)

      val cat = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("")
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse("")
      val rightAns = (json.getOrElse(null) \ "right-answer").asOpt[String].getOrElse("")
      val df = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0)
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse("")
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse("")
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse("")

      if ((cat != null) && (qid >= 0)) {
        val res: Boolean = quiz.addNewQuiz(qid, cat, question, rightAns, ans1, ans2, ans3, df)
        if (res) {
          // check and update category
          if (category.addNewCategory(cat, "")) {
            Logger.info("Add category: %s successfully" format cat)
          }
          Ok(""" {"qid": %d} """ format qid)
        } else {
          Ok(FailedStatus)
        }
      } else {
        BadRequest(FailedStatus)
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Update quiz's info. Json body contains: category, question, right answer, df, answer 1, answer 2, answer 3.
   * @return quiz's id if success, otherwise Failed status, bad request.
   */
  def updateQuiz = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
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
          Ok("""{"qid": %d}""" format qid)
        else
          Ok(FailedStatus)
      } else {
        BadRequest("Cannot update quiz %s" format json)
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }
}
