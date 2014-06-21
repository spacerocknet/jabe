package controllers

import scala.util.Random

import play.api.mvc._
import play.api.mvc.Controller

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

import models.QuizRequest
import models.GameResult
import models.QuAn


object QuizConttroller extends Controller {

  val json = Json.obj(
        "key1" -> "value1",
        "key2" -> Json.obj(
            "key21" -> 123,
            "key22" -> true,
            "key23" -> Json.arr("alpha", "beta", "gamma"),
            "key24" -> Json.obj(
                         "key241" -> 234.123,
                         "key242" -> "value242"
                       )
             ),
        "key3" -> 234
   )

  val jsonConfig = Json.obj(
                      "categories" -> Json.arr("Movies", "Spors", "Geographies", "Musics"),
                      "battles_per_game" -> 6
                   )

  def hello = Action {
    Ok(json)
  }

  
  def categories = Action {
       val json = Json.arr(
                      "Movies",
                      "Sports",
                      "Geographies",
                      "Musics"
                  )
        Ok(json)
  }

  def config = Action {
     Ok(jsonConfig)
  }


  implicit val quizReqReads: Reads[QuizRequest] = (
    (JsPath \ "userId").read[Long] and
    (JsPath \ "category").read[String] and
    (JsPath \ "num").read[Int]
  )(QuizRequest.apply _)
  

  def quizRequest = Action(parse.json) { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("minh"))
    Logger.info("start")
    try {
      val quizReqJson = request.body
      println(quizReqJson)
      val quizReq = quizReqJson.as[QuizRequest]
      println("userId: " + quizReq.userId)
      println("category: " + quizReq.category)
      println("num: " + quizReq.num)

      if (!QuAn.getQuestions.contains(quizReq.category)) {
         Ok(JsArray())
      } else {
         val r: Random = new Random()
         var seq = Seq[JsObject]()
         var i: Int = 0;
         val questions = QuAn.getQuestions(quizReq.category).toArray[QuAn]
         val subSet = questions
         for(i <- 0 until quizReq.num) {
            val index = Math.abs(r.nextInt()) % subSet.size
            var q = subSet(index) 
            var jsonObj = Json.obj("category" -> q.category,
                                   "qid" -> q.qid,
                                   "questions" -> q.question,
                                   "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3)
                                  )
            seq = seq:+ jsonObj
         }

         Ok(JsArray(seq))
      }
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }


  implicit val gameResultReads: Reads[GameResult] = (
    (JsPath \ "userId").read[Long] and
    (JsPath \ "result").read[String] 
  )(GameResult.apply _)
  
  
  def gameResult = Action(parse.json) { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("minh"))
    Logger.info("start")
    try {
      val gameResultJson = request.body
      println(gameResultJson)
      val gameResult = gameResultJson.as[GameResult]
      println("userId: " + gameResult.userId)
      println("category: " + gameResult.result)

      Ok("Ok")
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }  


  implicit val quanReads: Reads[QuAn] = (
    (JsPath \ "qid").read[Long] and
    (JsPath \ "category").read[String] and
    (JsPath \ "question").read[String] and
    (JsPath \ "correctAns").read[String] and
    (JsPath \ "ans1").read[String] and
    (JsPath \ "ans2").read[String] and
    (JsPath \ "ans3").read[String] 
  )(QuAn.apply _)


  def save = Action(parse.json) { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
    Logger.info("start")
    try {
      val quanJson = request.body
      println(quanJson)
      val quan = quanJson.as[QuAn]
      println("qid: " + quan.qid)
      println("category: " + quan.category)

      QuAn.save(quan)      
      Ok("Ok")
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
  }

  
  def savetest = Action { implicit request =>
    println(s"*** content-type: ${request.contentType}")
    println(s"*** headers: ${request.headers}")
    println(s"*** body: ${request.body}")
    println(s"*** query string: ${request.rawQueryString}") 
    Ok("Ok")
  }


  implicit object QuAnWrites extends Writes[QuAn] {
    def writes(p: QuAn) = Json.obj(
      "qid" -> Json.toJson(p.qid),
      "category" -> Json.toJson(p.category),
      "question" -> Json.toJson(p.question),
      "correctAns" -> Json.toJson(p.correctAns),
      "ans1" -> Json.toJson(p.ans1),
      "ans2" -> Json.toJson(p.ans2),
      "ans3" -> Json.toJson(p.ans3)
    )
  }

  /**
   * Returns details of the given quan.
   */
  def details(cat: String, qid: Long) = Action {
    QuAn.findById(cat, qid).map { quan =>
      Ok(Json.toJson(quan))
    }.getOrElse(NotFound)
  }
  
}
