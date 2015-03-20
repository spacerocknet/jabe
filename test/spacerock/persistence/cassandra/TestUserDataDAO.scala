package spacerock.persistence.cassandra

import controllers.TestGlobal
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
object TestUserDataDAO {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val user: UserData = new UserDataDAO

      user.addDeviceInfo("uid 1", "platform 1", "os 1", "model 1", "phone 1", "duuid 1")
      user.addDeviceInfo("uid 2", "platform 2", "os 2", "model 2", "phone 2", "duuid 2")
      user.addDeviceInfo("uid 3", "platform 3", "os 3", "model 3", "phone 3", "duuid 3")
      user.addDeviceInfo("uid 4", "platform 4", "os 4", "model 4", "phone 4", "duuid 4")
      user.addDeviceInfo("uid 5", "platform 5", "os 5", "model 5", "phone 5", "duuid 5")

      user.addBasicInfo("uid 1", "username 1", "firstname 1", "lastname 1", "email 1", "fbid 1",
        "loc 1", "region 1", "country 1", "appname 1")

      user.addBasicInfo("uid 2", "username 2", "firstname 2", "lastname 2", "email 2", "fbid 2",
        "loc 2", "region 12", "country 2", "appname 2")

      user.addBasicInfo("uid 3", "username 3", "firstname 3", "lastname 3", "email 3", "fbid 3",
        "loc 1", "region 3", "country 1", "appname 1")

      user.addBasicInfo("uid 4", "username 14", "firstname 4", "lastname 1", "email5", "fbid 1",
        "loc 1", "region 1", "country 6", "appname 1")

      user.changeDevice("uid 5", "platform 5", "os 9", "model 10", "phone 100", "duuid 99")

      println(user.getAllUsers())

      println(user.getInfoByUID("uid1"))

      println(user.getInfoByUsername("username 1"))

      println(user.updateLastSeenField("uid 1"))
    }
  }
}
