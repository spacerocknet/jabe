package spacerock.utils

import scala.collection.mutable.ListBuffer
/**
 * Created by william on 2/23/15.
 */
object StaticVariables {
  // id block
  // list of uid that jabe server granted from db
  val freeIds: ListBuffer[String] = new ListBuffer[String]
  // server id
  val serverId: Int = 1
  // encrypt key
  var pk: String = "kfjfklsdajflksajfdlksafjslka"
}
