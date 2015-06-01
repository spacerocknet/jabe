package spacerock.usergame

import java.util.concurrent.BlockingQueue
import scaldi.Injector
import spacerock.persistence.cassandra.OpenGameSession
import scaldi.Injectable
import models.OpenGameSessionModel
import java.util.concurrent.TimeUnit

class GameSessionScanner[T](implicit inj: Injector) extends Runnable with Injectable  {
  implicit val openGameSessionDao = inject[OpenGameSession]
  
  def run() {
    //Source.fromFile(path, "utf-8").getLines.foreach { line =>
    //  queue.put(line)
    //}
    while (true) {
       var gameSessions : List[OpenGameSessionModel] = openGameSessionDao.getGameSessions(100)
       gameSessions.foreach { openGameSession =>
                               GameSessionUtil.openGameSessionQueue.put(openGameSession.gameSessionId)
                            }
       

       while (!GameSessionUtil.deletedGameSessionQueue.isEmpty()) {
            val gameSessionId = GameSessionUtil.deletedGameSessionQueue.take()
            openGameSessionDao.removeOpenGameSession(gameSessionId)
       }
       
       Thread sleep 10000
    }
  }
}
