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
   
   //only look at the 1st item - if that is the same user's gameSession, we stop and return -1 
   def takeAnOpenGameSession(uid : String) : OpenGameSessionModel = {
       var openGameSession : OpenGameSessionModel = openGameSessionQueue.peek()
       if (openGameSession == null)
         return new OpenGameSessionModel("-1")
       if (openGameSession.gameSessionId.indexOf(uid) != -1) {
          return new OpenGameSessionModel("-2")
       } else {
          openGameSessionQueue.take()
          deletedGameSessionQueue.put(openGameSession)
       }
      
       openGameSession
   }
   
   //provided that the input openGameSession is already in Cassandra
   def addAnOpenGameSession(openGameSession : OpenGameSessionModel) = {
      openGameSessionQueue.put(openGameSession)
   }
}
