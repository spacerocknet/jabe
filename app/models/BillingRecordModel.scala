package models

import java.util.Date

/**
 * Created by william on 2/24/15.
 */
case class BillingRecordModel (uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, discount: Float) {
  final val fmt: String = new String(
    """{
      |"uid" : "%s",
      |"timestamp" : "%s",
      |"game_id" : %d,
      |"sku-id" : %d,
      |"number-of-items" : %d,
      |"total-discount" : %.3f
      |}""".stripMargin)
  override def toString(): String = {
    fmt.format(uid, ts.toString, gameId, skuId, nItems, discount)
  }
}
