package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/7/15.
 */
object TestSkuController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val description = "des 6"
      val skuId = 20000
      val unitPrice = 101.5f
      val extraData = "extra data. hehe"
      val startTime = System.currentTimeMillis()
      val expiredTime = startTime + 10000000
      val discount = 0.2f

      println("Test get non exist sku")
      testGetSkuInfo(skuId)
      println("Test add new sku")
      val sku = testAddNewSku(description, unitPrice, startTime, expiredTime, extraData, discount)
      println("Test get existing sku: %d" format sku)
      testGetSkuInfo(sku)
    }
  }

  def testAddNewSku(description: String, unitPrice: Float, startTime: Long, expiredTime: Long,
                    extraData: String, discount: Float): Int = {
    val response = route(FakeRequest(POST, "/v1/sku/add")
      .withJsonBody(Json.obj(
      "description" -> description,
      "unit-price" -> unitPrice,
      "start-time" -> startTime,
      "expired-time" -> expiredTime,
      "extra-data" -> extraData,
      "discount" -> discount))).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
    val js = contentAsJson(response)
    val skuId: Int = (js \ "sku-id").asOpt[Int].getOrElse(-1)
    skuId
  }

  def testGetSkuInfo(skuId: Int): Unit = {
    val response = route(FakeRequest(GET, "/v1/sku/get/%s" format skuId)).get
    println(contentAsString(response))
    status(response) must equalTo(OK)
    contentType(response) must beSome.which(_ == "application/json")
  }
}

