package shared

import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import models.OpenGameSessionModel
import java.util.concurrent.LinkedBlockingQueue

object Shared {
   private var data: Int = 0

   def setData(d: Int) : Unit = data = 0 
   def getData : Int = data
}

