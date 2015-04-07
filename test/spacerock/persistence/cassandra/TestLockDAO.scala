package spacerock.persistence.cassandra

import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestLockDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val lock: CassandraLock = inject [CassandraLock]
      assert(lock.tryLock("uid"))
      assert(lock.tryLock("uid") == false)
      assert(lock.unlock("uid"))
      assert(lock.tryLock("uid"))
      assert(lock.unlock("uid"))
    }
  }
}
