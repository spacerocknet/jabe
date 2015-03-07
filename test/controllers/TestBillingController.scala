package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/5/15.
 */

object TestBillingController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val uid: String = "kljfdasku432i4"
      val gameId = 100
      val skuId = 10
      val ts = System.currentTimeMillis()
      val numItems = 1000
      val discount = 100.1f

      println("Test add new billing record")
      testAddNewBill(uid, gameId, ts, skuId, numItems, discount)

      println("Test get all billing record of a user in all games")
      testGetBillsByUidAllGames(uid, ts - 100000, ts + 10000)
      println("Test get all billing record of a user in a game")
      testGetBillsByUidInGame(uid, gameId, -1, -1)
    }
  }

  def testAddNewBill(uid: String, gameId: Int, ts: Long, skuId: Int, numItems: Int, discount: Float): Unit = {
    val response = route(FakeRequest(POST, "/v1/billing/add")
      .withJsonBody(Json.obj("uid" -> uid,
                             "game-id" -> gameId,
                             "timestamp" -> ts,
                             "sku-id" -> skuId,
                             "num-items" -> numItems,
                             "discount" -> discount))).get

    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }

  def testGetBillsByUidAllGames(uid: String, from: Long, to: Long): Unit = {
    val response = route(FakeRequest(POST, "/v1/billing/getallbyuid")
      .withJsonBody(Json.obj("uid" -> uid,
                             "from" -> from,
                             "to" -> to))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }

  def testGetBillsByUidInGame(uid: String, gameId: Int, from: Long, to: Long): Unit = {
    val response = route(FakeRequest(POST, "/v1/billing/getbyuidofgame")
      .withJsonBody(Json.obj("uid" -> uid,
      "game-id" -> gameId,
      "from" -> from,
      "to" -> to))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }
}


