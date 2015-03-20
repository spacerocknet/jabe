package controllers

import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import spacerock.persistence.cassandra.{Category, Quiz}
import spacerock.utils.{StaticVariables, IdGenerator}

import scala.collection.mutable
import scala.util.Random

class QuizController(implicit inj: Injector) extends Controller with Injectable {
  val category = inject[Category]
  val quiz = inject[Quiz]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")
  val idGenerator = inject [IdGenerator]
  implicit val quizFmt = Json.format[QuAnModel]

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
         val questions: mutable.Buffer[QuAnModel] = quiz.getQuizzesByCategory(cat, num).toBuffer
         if (questions != null) {
           for (i <- 0 until num) {
             if (questions.size > 0) {
               val index: Int = Math.abs(r.nextInt()) % questions.size
               val q = questions.remove(index)
               val jsonObj = Json.obj("category" -> q.category,
                 "qid" -> q.qid,
                 "question" -> q.question,
                 "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
                 "df" -> JsNumber(q.df)
               )
               seq = seq :+ jsonObj
             }
           }
         }
         Ok(JsArray(seq))
       }
    } catch {
      case e:Exception => {
        e.printStackTrace()
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
   * @return array of quizzes if success, otherwise Bad request
   */
  def getQuizByCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val catName = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("")
      if (catName != null) {
        var seq = Seq[JsObject]()
        val l: List[QuAnModel] = quiz.getQuizzesByCategory(catName, Constants.DEFAULT_RESULT_SIZE)
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
      } else {
        if (quiz.lastError == Constants.ErrorCode.ERROR_SUCCESS) {
          Ok(Json.obj())
        } else {
          Ok(Json.obj("status" -> quiz.lastError))
        }
      }
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Add new quiz to system. Json body contains: category, question, right answer, df, answer 1, answer 2, answer 3.
   * Quiz's id is generated from IdGenerator by using redis.
   * @return quiz's id if success, otherwise Bad request, failed status
   */
  def addNewQuiz = Action { request =>
    var retObj: JsObject = FailedStatus
    try {
      val json: Option[JsValue] = request.body.asJson
      val qid: Long = idGenerator.generateNextId(Constants.REDIS_QUIZ_ID_KEY)

      val cat = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("")
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse("")
      val rightAns = (json.getOrElse(null) \ "right_answer").asOpt[String].getOrElse("")
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
          retObj = Json.obj("qid" -> qid)
        }
      } else {
        Logger.warn("Bad request. %s" format json.toString)
        retObj = StaticVariables.InputErrorStatus
      }

    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }

  /**
   * Update quiz's info. Json body contains: category, question, right answer, df, answer 1, answer 2, answer 3.
   * @return quiz's id if success, otherwise Failed status, bad request.
   */
  def updateQuiz = Action { request =>
    var retObj: JsObject = FailedStatus
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
          retObj = Json.obj("qid" -> qid)
      } else {
        Logger.warn("Bad request. %s" format json)
        retObj = StaticVariables.InputErrorStatus
      }

    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }
}
