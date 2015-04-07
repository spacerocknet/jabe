package spacerock.persistence.cassandra

import java.util.Date

import models.SkuModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
object TestSkuDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val sku: Sku = inject [Sku]

      val s1: SkuModel = new SkuModel(1, "Description 1", 10.5f, new Date(System.currentTimeMillis() - 100052),
        new Date(System.currentTimeMillis() - 19900), "extra 1", 0.0f)

      val s2: SkuModel = new SkuModel(2, "Description 2", 20.5f, new Date(System.currentTimeMillis() - 200052),
        new Date(System.currentTimeMillis() - 29900), "extra 2", 20.0f)

      val s3: SkuModel = new SkuModel(3, "Description 3", 30.5f, new Date(System.currentTimeMillis() - 300052),
        new Date(System.currentTimeMillis() - 39900), "extra 3", 30.0f)

      val s4: SkuModel = new SkuModel(4, "Description 4", 40.5f, new Date(System.currentTimeMillis() - 400052),
        new Date(System.currentTimeMillis() - 49900), "extra 4", 40.0f)

      assert(sku.addNewSku(s1))
      assert(sku.addNewSku(s2))
      assert(sku.addNewSku(s3))
      assert(sku.addNewSku(s4))
      assert(sku.addNewSku(6, "Description 6", 60.5f, new Date(System.currentTimeMillis() - 600052),
        new Date(System.currentTimeMillis() - 69900), "extra 6", 60.0f))


      println(sku.getSkuInfo(1))
      println(sku.getSkuInfo(2))

      assert(sku.getSkuInfo(100) == null)
    }
  }
}
