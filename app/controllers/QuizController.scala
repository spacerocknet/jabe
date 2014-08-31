package controllers

import scala.collection.mutable.MutableList
import scala.collection.JavaConversions._

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Controller
import scaldi.Injectable
import scaldi.Injector

import scala.util.Random

import spacerock.persistence.TAppsConfig
import models._

class QuizController(implicit inj: Injector) extends Controller with Injectable {
  val appsConfigDao = inject [TAppsConfig]
  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")

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

  def hello = Action {
    Ok(json)
  }

  val jsonConfig = Json.obj(
                      "categories" -> Json.arr("Movies", "Spors", "Geographies", "Musics"),
                      "battles_per_game" -> 6
                   )
  
  def categories = Action {
       val appsConfig = appsConfigDao.getAppsConfiguration("asteroid")
       println("categories:::: " + appsConfig.categories)
       val json = Json.arr(
                      "Movies",
                      "Sports",
                      "Geographies",
                      "Musics"
                  )
                  
        Ok(json)
  }

  def config = Action {
     val appsConfig = appsConfigDao.getAppsConfiguration("asteroid")
     val categories = appsConfig.categories.split(",")
     var list = MutableList[String]()
     categories.foreach( i => list += i)
    
     val jsonConfig = Json.obj(
                      "categories" -> Json.toJson(list),
                      "battles_per_game" -> 6
                   )
                   
     Ok(jsonConfig)
  }
  
  def quizRequest = Action { request =>
     var result = OkStatus
     println(request);
     println(request.headers)
     println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
     
     try {
       val json: Option[JsValue] = request.body.asJson
       println("Body ::: ")
       println(request.body)
       println(json)
       val userId = (json.getOrElse(null) \ "userId").asOpt[String].getOrElse("")
       var category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("Movies")
       val num = (json.getOrElse(null) \ "num").asOpt[Int].getOrElse(1)
      
       println("userId: " + userId)
       println("category: " + category)
       println("num: " + num)
     
       category = QuAn.normalizeCat(category)

       if (!QuAn.hasDataForCategory(category)) {       
         Ok(JsArray())
       } else {
         val r: Random = new Random()
         var seq = Seq[JsObject]()
         var i: Int = 0;
         val questions = QuAn.getQuestions(category).toArray[QuAn]
         val subSet = questions
         for(i <- 0 until num) {
            val index = Math.abs(r.nextInt()) % subSet.size
            var q = subSet(index) 
            var jsonObj = Json.obj("category" -> q.category,
                                   "qid" -> q.qid,
                                   "question" -> q.question,
                                   "answers" -> Json.arr(q.correctAns, q.ans1, q.ans2, q.ans3),
                                   "df" -> JsNumber(q.df)
                                  )
            seq = seq:+ jsonObj
         }

         Ok(JsArray(seq))
       }
    } catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid EAN")
      }
    }
    
  }

  def gameResult = Action { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
    
    try {
      val json: Option[JsValue] = request.body.asJson
      println("Body ::: ")
      println(request.body)
      println(json)
      val userId = (json.getOrElse(null) \ "userId").asOpt[String].getOrElse("")
      val result = (json.getOrElse(null) \ "result").asOpt[String].getOrElse("0") 

      println("userId: " + userId)
      println("result: " + result)

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


  def save = Action { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
    Logger.info("start")
    
    try {
      val json: Option[JsValue] = request.body.asJson
      println("Body ::: ")
      println(request.body)
      println(json)
      val qid = (json.getOrElse(null) \ "qid").asOpt[Long].getOrElse(0L)
      val category = (json.getOrElse(null) \ "category").asOpt[String].getOrElse("Movies") 
      val question = (json.getOrElse(null) \ "question").asOpt[String].getOrElse("")
      val correctAns = (json.getOrElse(null) \ "correctAns").asOpt[String].getOrElse("")
      val ans1 = (json.getOrElse(null) \ "ans1").asOpt[String].getOrElse("")
      val ans2 = (json.getOrElse(null) \ "ans2").asOpt[String].getOrElse("")
      val ans3 = (json.getOrElse(null) \ "ans3").asOpt[String].getOrElse("")
      val df : Int   = (json.getOrElse(null) \ "df").asOpt[Int].getOrElse(0) 
     
      println("qid: " + qid)
      println("category: " + category)
      println("df: " + df)
      
      QuAn.save(new QuAn(qid, category, question, correctAns, ans1, ans2, ans3, df))      
      Ok("Ok")
    }
    catch {
      //case e:IllegalArgumentException => BadRequest("Product not found")
      case e:Exception => {
        Logger.info("exception = %s" format e)
        BadRequest("Invalid Input")
      }
    }
  }

  /**
   * Returns details of the given quan.
   */
  def details(cat: String, qid: Long) = Action { request =>
    println(request);
    println(request.headers)
    println(request.headers.get("Authorization").getOrElse("NoAuthorization"))
      
    QuAn.findById(cat, qid).map { quan =>
      val json = Json.obj(
           "qid" -> Json.toJson(quan.qid),
           "category" -> Json.toJson(quan.category),
           "question" -> Json.toJson(quan.question),
           "correctAns" -> Json.toJson(quan.correctAns),
           "ans1" -> Json.toJson(quan.ans1),
           "ans2" -> Json.toJson(quan.ans2),
           "ans3" -> Json.toJson(quan.ans3),
           "df" -> Json.toJson(quan.df)
         )
    
      Ok(json)
    }.getOrElse(NotFound)
  }
  
}
