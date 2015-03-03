package redis

import spacerock.persistence.{RedisWrapper, RedisWrapperDAO}

/**
 * Created by william on 3/1/15.
 */
object TestRedisWrapper {
  def main(args: Array[String]): Unit = {
    val rw: RedisWrapper = new RedisWrapperDAO()
    var l: Long = System.currentTimeMillis()

    println(rw.getNextId("uid"))
    println(System.currentTimeMillis() - l)
    l = System.currentTimeMillis()
    println(rw.getNextId("uid"))
    println(System.currentTimeMillis() - l)
    l = System.currentTimeMillis()
    println(rw.getNextId("uid"))
    println(System.currentTimeMillis() - l)
    l = System.currentTimeMillis()
    println(rw.getNextId("uid"))
    println(System.currentTimeMillis() - l)
    l = System.currentTimeMillis()
    println(rw.getValueFromKey("uid"))

  }
}
