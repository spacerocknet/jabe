package spacerock.persistence.cassandra

import java.util.Date

import models.BillingRecordModel
import scaldi.Module
/**
 * Created by william on 2/28/15.
 */
object TestBillingDAO {

//  def main(args: Array[String]): Unit = {
//    val billing: Billing = new BillingDAO()
//    // add
//
//    // def addNewBill(br: BillingRecord): Boolean
//    // def addNewBill(uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, totalDiscount: Float): Boolean
//    var b: BillingRecordModel = new BillingRecordModel("uid1", new Date(), 1, 1, 10, 10.05f)
//    if (!billing.addNewBill(b))
//      println("error")
//    else
//      println("insert success")
//    b = new BillingRecordModel("uid1", new Date(), 2, 1, 1, 10.05f)
//    if (!billing.addNewBill(b))
//      println("error")
//    else
//      println("insert success")
//    b = new BillingRecordModel("uid2", new Date(), 3, 1, 80, 10.05f)
//    if (!billing.addNewBill(b))
//      println("error")
//    else
//      println("insert success")
//    b = new BillingRecordModel("uid7", new Date(), 1, 1, 90, 10.05f)
//    if (!billing.addNewBill(b))
//      println("error")
//    else
//      println("insert success")
//
//
//    // def getAllBillsOfUser(uid: String): List[BillingRecord]
//    var res: List[BillingRecordModel] = billing.getAllBillsOfUser("uid1")
//    println(res.toString())
//    res = billing.getAllBillsOfUserWithDate("uid1", new Date(System.currentTimeMillis() - 1000 * 100), new Date(System.currentTimeMillis()))
//    println(res.toString())
//    res = billing.getBillsOfUserFromGame("uid1", 1)
//    println(res.toString())
//    // def getAllBillsOfUserWithDate(uid: String, from: Date, to: Date): List[BillingRecord]
//    // def getBillsOfUserFromGame(uid: String, gameId: Int): List[BillingRecord]
//
//    //
//    billing.close()
//  }
}
