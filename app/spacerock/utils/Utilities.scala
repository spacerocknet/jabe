package spacerock.utils

import java.net.InetAddress

/**
 * Created by william on 3/20/15.
 */
object Utilities {
  def ipToInt(iNetAddress: InetAddress): Int = {
    var result:Int = 0
    for (b: Byte <- iNetAddress.getAddress)
    {
      result = result << 8 | (b & 0xFF)
    }
    result
  }
}
