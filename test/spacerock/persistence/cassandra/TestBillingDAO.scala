package spacerock.persistence.cassandra

import java.util.Date

import models.BillingRecordModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestBillingDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val billing = inject [Billing]

      var b: BillingRecordModel = new BillingRecordModel("uid1", new Date(), 1, 1, 10, 10.05f)
      assert (billing.addNewBill(b))

      b = new BillingRecordModel("uid1", new Date(), 2, 1, 1, 10.05f)
      assert(billing.addNewBill(b))

      b = new BillingRecordModel("uid2", new Date(), 3, 1, 80, 10.05f)
      assert(billing.addNewBill(b))

      b = new BillingRecordModel("uid7", new Date(), 1, 1, 90, 10.05f)
      assert(billing.addNewBill(b))

      var res: List[BillingRecordModel] = billing.getAllBillsOfUser("uid1")
      assert(res != null)
      println(res.toString())
      res = billing.getAllBillsOfUser("uid1000")
      assert(res != null)
      println(res.toString)
      res = billing.getAllBillsOfUserWithDate("uid1", new Date(System.currentTimeMillis() - 1000 * 100), new Date(System.currentTimeMillis()))
      assert(res != null)
      println(res.toString())
      res = billing.getBillsOfUserFromGame("uid1", 1)
      assert(res != null)
      println(res.toString())
    }
  }
}
