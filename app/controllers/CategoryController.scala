package controllers

import models._
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence.cassandra.Category

class CategoryController (implicit inj: Injector) extends Controller with Injectable {
  val category = inject[Category]

  val OkStatus = Json.obj("status" -> "OK")
  val FailedStatus = Json.obj("status" -> "Failed")

  /**
   * Get all category from system
   * @return Ok status with list of all category, or failed status otherwise
   */
  def getAllCategory = Action {
    val list: List[CategoryModel] = category.getAllCategories()
    if (list != null)
      Ok(Json.toJson(list))
    else
      Ok(FailedStatus)
  }

  /**
   * Get category info by name
   * @param catName category name
   * @return returned json object contains fields: category, description, game-id. This will be returned is success,
   *         otherwise empty object, bad request
   */
  def getCategoryByName (catName: String) = Action {
    try {
      if (catName != null) {
        val cat: CategoryModel = category.getCategoryByName(catName)
        if (cat != null) {
          val jsonObj = Json.obj("category" -> cat.category,
            "description" -> cat.description,
            "game-id" -> Json.toJson(cat.gameIds))
          Ok(jsonObj)
        } else {
          Ok("{}")
        }
      } else {
        BadRequest(FailedStatus)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Update category information. Json body contains: category, game-id (single value), description
   * @return Ok success status if success, or Ok
   */
  def updateCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val gameId: Int = (json.getOrElse(null) \ "game-id").asOpt[Int].getOrElse(-1)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.updateCategory(catName, gameId, description))
          Ok(OkStatus)
        else
          Ok(FailedStatus)
      } else {
        BadRequest(FailedStatus)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }

  /**
   * Add new category to system. In fact, category model contains game-id list,
   * but these will be inserted when game is added
   * @return Ok status if success, failed status, bad request if not
   */
  def addCategory = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.addNewCategory(catName, description))
          Ok(OkStatus)
        else
          Ok(FailedStatus)
      } else {
        BadRequest(FailedStatus)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        BadRequest("Invalid EAN")
      }
    }
  }
}
