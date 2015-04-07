package spacerock.persistence.cassandra

import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestCategoryDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val cat: Category = inject [Category]
      assert(cat.addNewCategory("category 1", "description for category 1"))
      assert(cat.addNewCategory("category 2", "description for category 2"))
      assert(cat.addNewCategory("category 3", "description for category 3"))
      assert(cat.addNewCategory("category 5", "description for category 5"))
      assert(cat.updateCategory("category 1", 1, "description for category 1 (updated)"))
      assert(cat.updateCategory("category 1", 2, "description for category 1 (updated)"))
      println(cat.getAllCategories())
      println(cat.getCategoryByName("category 2"))

    }
  }
}
