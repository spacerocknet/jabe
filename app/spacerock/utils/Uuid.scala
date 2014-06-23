package spacerock.utils

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import scaldi.Injector
import scaldi.Injectable
import java.util.concurrent.atomic.AtomicLong
import play.api.Logger

trait UuidGenerator {
    def generate() : String
}


class UuidMacBasedGenerator(implicit inj: Injector) extends UuidGenerator with Injectable {
    val counter = new AtomicLong
    var machineMac: String = ""
    init()
    
    def init() : Unit = {
      try {
        var ip = InetAddress.getLocalHost()
        
        //Logger.info("ip: " + ip);
        //val network = NetworkInterface.getByInetAddress(ip)
        //Logger.info("netowkr : " + network)
        
        //val mac : Array[Byte] = network.getHardwareAddress()
        
        //can we just do new String(mac)?
        //val sb = new StringBuilder()
        //var i = 0
        //for (i <- 0 to (mac.length - 1)) {
        //    sb.append(mac(i));        
        //}
        //machineMac = sb.toString()
        machineMac = ip.toString()
        machineMac = machineMac.replaceAll("/","")
      } catch {
         case e:Exception => {
           Logger.info("exception = %s" format e)
         }
      }
    }
    
    
    def generate() : String = {
       Logger.info("Mac: " + machineMac);
       return machineMac + System.nanoTime().toString() + counter.incrementAndGet()
    }
    
}