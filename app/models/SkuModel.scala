package models

import java.util.Date

/**
 * Created by william on 2/24/15.
 */
case class SkuModel (skuId: Int, description: String, unitPrice: Float,
                 startTime: Date, expiredTime: Date, extraData: String, discount: Float) {
  final val fmt: String =
    """
      |{
      |"sku-id" : %d,
      |"description" : "%s",
      |"unit-price" : %.3f,
      |"start-time" : "%s",
      |"expired_time" : "%s",
      |"extra-data" : "%s",
      |"discount" : %.3f
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(skuId, description, unitPrice, startTime.toString, expiredTime.toString,
              extraData, discount)
  }
}