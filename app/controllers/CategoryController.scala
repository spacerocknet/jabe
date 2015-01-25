package controllers

import models._
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.persistence.{NewCategory, NewQuiz, NewUserData}

class CategoryController (implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [NewUserData]
  val category = inject[NewCategory]
  val quiz = inject[NewQuiz]

  def getAllCategory = Action {
    println(category.getAllCategories().toString())
    Ok(category.getAllCategories().toString())
  }

  def getCategoryByName = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)

      if (catName != null) {
        val cat: Category = category.getCategoryByName(catName)
        val jsonObj = Json.obj("category" -> cat.category,
          "description" -> cat.description)
        Ok(jsonObj)
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
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.updateCategory(catName, description))
          Ok("Update category successful")
        else
          Ok("Cannot update category")
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
      val catName = (json.getOrElse(null) \ "category").asOpt[String].orNull(null)
      val description = (json.getOrElse(null) \ "description").asOpt[String].getOrElse("")

      if (category != null) {
        if (category.addNewCategory(catName, description))
          Ok("Add new category successful")
        else
          Ok("Cannot add new category")
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
