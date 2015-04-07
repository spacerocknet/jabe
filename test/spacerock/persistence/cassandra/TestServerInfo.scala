package spacerock.persistence.cassandra

import java.net.InetAddress

import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/21/15.
 */
object TestServerInfo extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val sInfo: ServerInfo = inject [ServerInfo]
      val ip: InetAddress = InetAddress.getByName("google.com")

      assert(sInfo.insertOrUpdateServerInfo(ip, 100))
      assert(sInfo.getSeqInfo(ip) == 700)
      println("Finish")
    }
  }
}

