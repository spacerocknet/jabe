package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/5/15.
 */

object TestQuizController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val question1 = "question 1"
      val question2 = "question 2"
      val cat1 = "cat 1"
      val cat2 = "cat 2"
      val righAns = "right ans"
      val df = 1
      val ans1 = "ans 1"
      val ans2 = "ans 2"
      val ans3 = "ans 3"

      val qid1 = testAddNewQuiz(question1, cat1, righAns, df, ans1, ans2, ans3)
      val qid2 = testAddNewQuiz(question2, cat1, righAns, df, ans1, ans2, ans3)

      testGetQuizById(qid1)
      testGetQuizById(qid2)

      testGetQuizByCategory(cat1)
      testGetQuizByCategory(cat2)

      testGetAllQuiz()

      testRequestQuizzes(cat1, 1)
      testRequestQuizzes(cat1, 10)
      testRequestQuizzes(cat2, 1)
      testRequestQuizzes(cat2, 110)
    }
  }

  def testAddNewQuiz(question: String, cat: String, rightAns: String,
                      df: Int, ans1: String, ans2: String, ans3: String): Int = {
    val response = route(FakeRequest(POST, "/v1/quiz/add")
      .withJsonBody(Json.obj("question" -> question,
      "category" -> cat,
      "right-answer" -> rightAns,
      "df" -> df,
      "ans1" -> ans1,
      "ans2" -> ans2,
      "ans3" -> ans3))).get
    println(contentAsString(response))
    val gameId: Int = (contentAsJson(response) \ "qid").asOpt[Int].getOrElse(-1)
    gameId
  }

  def testGetQuizById(qid: Int): Unit = {
    val response = route(FakeRequest(GET, "/v1/quiz/byid/%d" format qid)).get
    println(contentAsString(response))
  }

  def testGetQuizByCategory(category: String): Unit = {
    val response = route(FakeRequest(POST, "/v1/quiz/bycat")
      .withJsonBody(Json.obj("category" -> category))).get
    println(contentAsString(response))
  }

  def testGetAllQuiz(): Unit = {
    val response = route(FakeRequest(GET, "/v1/quiz/all")).get
    println(contentAsString(response))
  }

  def testRequestQuizzes(catName: String, num: Int): Unit = {
    val response = route(FakeRequest(POST, "/v1/quiz/request")
      .withJsonBody(Json.obj("category" -> catName,
      "num" -> num))).get
    println(contentAsString(response))
  }
}

