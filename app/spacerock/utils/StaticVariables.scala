package spacerock.utils

import java.net.InetAddress

import scala.collection.mutable.ListBuffer

import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper

/**
 * Created by william on 2/23/15.
 */
object StaticVariables {
  // id block
  // list of uid that jabe server granted from db
  val freeIds: ListBuffer[String] = new ListBuffer[String]
  // server id
  val serverId: Int = 1
  var serverIp: InetAddress = InetAddress.getLocalHost
  var serverIpInt: Int = -1

  val pk: String = "kasjFSDSd219478898vcxzqwerhj3JKHSH"

  // response's status
  val OkStatus = Json.obj("status" -> 0)
  val BackendErrorStatus = Json.obj("status" -> 1)
  val DbErrorStatus = Json.obj("status" -> 2)
  val InputErrorStatus = Json.obj("status" -> 3)
  val WrongInputValueStatus = Json.obj("status" -> 4)
  val NoIdStatus = Json.obj("status" -> 5)


  val errorCodeMap = Map (0 -> "OK",
                          1 -> "Backend service unavailable",
                          2 -> "Database is down",
                          3 -> "Bad input format",
                          4 -> "Wrong input value",
                          5 -> "No id")
                            
}
