package controllers

import models._
import play.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence.cassandra.Category
import spacerock.utils.StaticVariables

class CategoryController (implicit inj: Injector) extends Controller with Injectable {
  val category = inject[Category]
  implicit val catFmt = Json.format[CategoryModel]
  final val EMPTY_JSON: JsObject = Json.obj()

  /**
   * Get all category from system
   * @return Ok status with list of all category, or failed status otherwise
   */
  def getAllCategory = Action {
    val list: List[CategoryModel] = category.getAllCategories()

    if (list != null) {
      Ok(Json.toJson(list))
    } else {
      Logger.warn("Cannot get all category. Please check database again")
      Ok(StaticVariables.DbErrorStatus)
    }
  }

  /**
   * Get category info by name
   * @return returned json object contains fields: category, description, game-id. This will be returned is success,
   *         otherwise empty object, bad request
   */
  def getCategoryByName = Action { request =>
    var retObj: JsObject = null
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      if (catName != null) {
        val cat: CategoryModel = category.getCategoryByName(catName)
        if (cat != null) {
          val jsonObj = Json.obj("category" -> cat.category,
            "description" -> cat.description,
            "game-id" -> Json.toJson(cat.gameIds))
          retObj = jsonObj
        } else {
          Logger.warn("Get category by name failed: %s" format catName)
          retObj = EMPTY_JSON
        }
      } else {
        retObj = StaticVariables.InputErrorStatus
      }
      Ok(retObj)
    } catch {
      case e: Exception => {
        Logger.error("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Update category information. Json body contains: category, game-id (single value), description
   * @return Ok success status if success, or failed status, request body error otherwise
   */
  def updateCategory = Action { request =>
    var retObj: JsObject = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val gameId: Int = (json.getOrElse(null) \ "game_id").asOpt[Int].getOrElse(-1)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.updateCategory(catName, gameId, description)) {
          retObj = StaticVariables.OkStatus
        } else {
          Logger.warn("Cannot update category. %s" format json.toString)
          retObj = StaticVariables.DbErrorStatus
        }
      } else {
        retObj = StaticVariables.InputErrorStatus
        Logger.warn("Request body error. %s" format json.toString)
      }
    } catch {
      case e: Exception => {
        Logger.error("exception e = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }

  /**
   * Add new category to system. In fact, category model contains game-id list,
   * but these will be inserted when game is added
   * @return Ok status if success, failed status, bad request if not
   */
  def addCategory = Action { request =>
    var retObj: JsObject = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.addNewCategory(catName, description)) {
          retObj = StaticVariables.OkStatus
        } else {
          Logger.warn("Cannot add category to database. %s" format catName, description)
          retObj = StaticVariables.DbErrorStatus
        }
      } else {
        retObj = StaticVariables.InputErrorStatus
        Logger.warn("Request body error. %s" format json.toString)
      }

    } catch {
      case e: Exception => {
        Logger.error("exception e = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }
}
