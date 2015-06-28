package spacerock.usergame

import spacerock.persistence.cassandra.OpenGameSession
import models.OpenGameSessionModel
import java.util.concurrent.LinkedBlockingQueue

object GameSessionUtil {

   val openGameSessionQueue = new LinkedBlockingQueue[OpenGameSessionModel]() 
   val deletedGameSessionQueue = new LinkedBlockingQueue[OpenGameSessionModel]() 
   
   def takeAnOpenGameSession : OpenGameSessionModel = {
      var openGameSession : OpenGameSessionModel = openGameSessionQueue.peek()
      if (openGameSession != null) {
         openGameSessionQueue.take()
         deletedGameSessionQueue.put(openGameSession)
      } else openGameSession = new OpenGameSessionModel("-1")
      
      return openGameSession
   }
   
   //provided that the input openGameSession is already in Cassandra
   def addAnOpenGameSession(openGameSession : OpenGameSessionModel) = {
      openGameSessionQueue.put(openGameSession)
   }
}
