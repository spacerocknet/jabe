package spacerock.usergame

import java.util.concurrent.LinkedBlockingQueue

object GameSessionUtil {
   val openGameSessionQueue = new LinkedBlockingQueue[String]() 
   val deletedGameSessionQueue = new LinkedBlockingQueue[String]() 
}