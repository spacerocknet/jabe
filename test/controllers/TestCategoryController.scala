package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/5/15.
 */

object TestCategoryController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      println("Test get all category")
      testGetAllCategory
      val catName = "cat 2"
      val description1 = "des 2"
      val gameId = 2
      println("Test add new category")
      testAddNewCategory(catName, description1)
      println("Test get new category by name (valid)")
      testGetCategoryByName(catName, true)
      println("Test get new category by name (invalid)")
      testGetCategoryByName("cafdsa", false)

      println("Test update category")
      testUpdateCategoryInfo(catName, gameId, description1)

      println("Test get new category by name (valid, after update)")
      testGetCategoryByName(catName, true)
    }
  }

  def testGetAllCategory(): Unit = {
    val response = route(FakeRequest(GET, "/v1/cat/all")).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }

  def testGetCategoryByName(catName: String, isExists: Boolean): Unit = {
    val response = route(FakeRequest(POST, "/v1/cat/getbyname")
      .withJsonBody(Json.obj("category" -> catName))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
    if (isExists) {
      contentAsString(response) must contain("category")
    } else {
      contentAsString(response) must contain("{}")
    }
  }

  def testUpdateCategoryInfo(catName: String, gameId: Int, description: String): Unit = {
    val response = route(FakeRequest(POST, "/v1/cat/update")
      .withJsonBody(Json.obj("category" -> catName, "game-id" -> gameId, "description" -> description))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }

  def testAddNewCategory(catName: String, description: String): Unit = {

    // valid
    val response = route(FakeRequest(POST, "/v1/cat/add")
      .withJsonBody(Json.obj("category" -> catName, "description" -> description))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }

}
