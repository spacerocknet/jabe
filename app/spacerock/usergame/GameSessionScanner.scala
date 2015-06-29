package spacerock.usergame

import java.util.concurrent.BlockingQueue
import scaldi.Injector
import spacerock.persistence.cassandra.OpenGameSession
import scaldi.Injectable
import models.OpenGameSessionModel
import java.util.concurrent.TimeUnit
import play.api.Logger

class GameSessionScanner(implicit inj: Injector) extends Runnable with Injectable  {
	implicit val openGameSessionDao = inject[OpenGameSession]
  var init : Boolean = false
  
  def run() {
		//Source.fromFile(path, "utf-8").getLines.foreach { line =>
		//  queue.put(line)
		//}
		Logger.info("Running GameSessionService ...")
    if (!init) {
        //openGameSessionDao.addNewOpenGameSession("opengamesession_1")
        //openGameSessionDao.addNewOpenGameSession("opengamesession_2")
        //openGameSessionDao.addNewOpenGameSession("opengamesession_3")
        //openGameSessionDao.addNewOpenGameSession("opengamesession_4")
        init = true
    }
    
		while (!GameSessionUtil.deletedGameSessionQueue.isEmpty()) {
			 val gameSession = GameSessionUtil.deletedGameSessionQueue.take()
			 openGameSessionDao.removeOpenGameSession(gameSession.gameSessionId)
		}

    if (GameSessionUtil.openGameSessionQueue.size() == 0) {
		    var gameSessions : List[OpenGameSessionModel] = openGameSessionDao.getGameSessions(100)
			  gameSessions.foreach { openGameSession =>
				   GameSessionUtil.openGameSessionQueue.put(openGameSession)
				   Logger.info("Adding GameSession : " + openGameSession.gameSessionId)
			  }
     }

	}

}
