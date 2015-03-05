package spacerock.utils

import org.apache.commons.codec.binary.Base64

/**
 * Created by william on 3/5/15.
 */
object CustomizedRandom {

  private val random = {
    val rnd = new java.security.SecureRandom()
    rnd.setSeed(rnd.generateSeed(55))
    rnd
  }

  def nextString(numBytes: Int): String = {
    val bytes = new Array[Byte](numBytes)
    random.nextBytes(bytes)
    new String(bytes, "UTF-8")
  }

  def nextBase64String(numBytes:Int):String = {
    val bytes = new Array[Byte](numBytes)
    random.nextBytes(bytes)
    val encodedBytes = Base64.encodeBase64(bytes)
    new String(encodedBytes, "UTF-8")
  }

  def nextLong: Long = random.nextLong()

  def nextInt: Int = random.nextInt()

  def nextInt(seed: Int): Int = random.nextInt(seed)

}