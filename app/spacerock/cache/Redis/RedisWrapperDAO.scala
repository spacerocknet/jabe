package spacerock.cache.Redis

import play.Logger
import redis.clients.jedis.Jedis
import scaldi.{Injectable, Injector}

/**
 * Created by william on 1/13/15.
 */

trait RedisWrapper {
  def getValueFromKey(key: String): Long
  def getNextId(key: String): Long
  def close(): Unit
}

class RedisWrapperDAO (implicit inj: Injector) extends RedisWrapper with Injectable {
  final val TIME_OUT = 10000
  val jedis: Jedis = connect("127.0.0.1", 10001)

  def connect(host: String, port: Int): Jedis = {
    val j: Jedis = new Jedis(host, port, TIME_OUT);
    if (j == null) {
      Logger.error("Cannot connect to redis")
      return null
    }
    j
  }

  override def getValueFromKey(key: String): Long = {
    if (jedis != null) {
      jedis.get(key).toLong
    } else {
      -1
    }
  }

  override def getNextId(key: String): Long = {
    if (jedis != null) {
      jedis.incr(key)
    } else {
      -1
    }
  }

  override def close(): Unit = {
    if (jedis != null)
      jedis.close()
  }
}


