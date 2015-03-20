package spacerock.persistence.cassandra

import controllers.TestGlobal
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestLockDAO {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val lock: CassandraLock = new CassandraLockDAO
      println(lock.tryLock("uid"))
      println(lock.tryLock("uid"))
      lock.unlock("uid")
      println(lock.tryLock("uid"))
      lock.unlock("uid")
    }
  }
}
