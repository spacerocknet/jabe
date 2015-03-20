package spacerock.persistence.cassandra

import controllers.TestGlobal
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestCategoryDAO {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val cat: Category = new CategoryDAO
      println(cat.addNewCategory("category 1", "description for category 1"))
      println(cat.addNewCategory("category 2", "description for category 2"))
      println(cat.addNewCategory("category 3", "description for category 3"))
      println(cat.addNewCategory("category 5", "description for category 5"))
      cat.updateCategory("category 1", 1, "description for category 1 (updated)")
      cat.updateCategory("category 1", 2, "description for category 1 (updated)")
      println(cat.getAllCategories())
      println(cat.getCategoryByName("category 2"))

    }
  }
}
