package spacerock.usergame

import org.specs2.mutable._
import play.api.GlobalSettings
import scaldi.Injectable
import scaldi.Injector
import scaldi.Module
import scaldi.play.ScaldiSupport
import spacerock.persistence.cassandra._
import models.OpenGameSessionModel
import models.UserGameSessionModel
import modules.UserModule



class UserGameSpec extends Specification with Injectable {

  implicit val injector = new UserModule

  "The CassandraDAO" should {
   
     "Test OpenGameSession" in {
        
        val openGameSessionDao = inject[OpenGameSession]
        openGameSessionDao.addNewOpenGameSession("opengamesession_1")
        openGameSessionDao.addNewOpenGameSession("opengamesession_2")
        openGameSessionDao.addNewOpenGameSession("opengamesession_3")
        openGameSessionDao.addNewOpenGameSession("opengamesession_4")
        
        val openGameSessionScanner = new GameSessionScanner
        new Thread(openGameSessionScanner).start()
        
        Thread sleep 1000
        
        var gameSessions : List[OpenGameSessionModel] = openGameSessionDao.getGameSessions(10)
        gameSessions must have size(4)
        
        var openGameId = GameSessionUtil.openGameSessionQueue.take()
        GameSessionUtil.deletedGameSessionQueue.put(openGameId)
        
        openGameId = GameSessionUtil.openGameSessionQueue.take()
        GameSessionUtil.deletedGameSessionQueue.put(openGameId)
        
        openGameId = GameSessionUtil.openGameSessionQueue.take()
        GameSessionUtil.deletedGameSessionQueue.put(openGameId)
        
        openGameId = GameSessionUtil.openGameSessionQueue.take()
        GameSessionUtil.deletedGameSessionQueue.put(openGameId)
        
        Thread sleep 11000
        
        gameSessions = openGameSessionDao.getGameSessions(10)
        gameSessions must have size(0)
     }
  }
  
}