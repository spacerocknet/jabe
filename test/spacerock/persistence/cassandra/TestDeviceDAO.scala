package spacerock.persistence.cassandra

import java.util.Date

import models.DeviceModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 2/28/15.
 */
object TestDeviceDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val device: Device = inject [Device]
      val d1: DeviceModel = new DeviceModel("device 1", new Date(), "uid 1", "os 1", "platform 1",
        "model 1", "phone 1")
      val d2: DeviceModel = new DeviceModel("device 2", new Date(), "uid 2", "os 2", "platform 2",
        "model 2", "phone 2")
      val d3: DeviceModel = new DeviceModel("device 3", new Date(), "uid 1", "os 3", "platform 3",
        "model 3", "phone 1")
      val d5: DeviceModel = new DeviceModel("device 5", new Date(), "uid 1", "os 5", "platform 5",
        "model 5", "phone 5")

      assert(device.addNewDevice(d1))
      assert(device.addNewDevice(d2))
      assert(device.addNewDevice(d3))
      assert(device.addNewDevice(d5))

      assert(device.addNewDevice("device 6", new Date(), "uid 6", "os 6", "platform 6",
        "model 6", "phone 6"))

      println(device.getInfoByDuuid("device 1"))

      println(device.getInfoByPhone("phone 1"))

      println(device.getInfoByPhone("phonr 19"))

      println(device.getInfoByUid("uid 1"))

    }
  }
}
