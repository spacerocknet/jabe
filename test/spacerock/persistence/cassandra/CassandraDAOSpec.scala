package spacerock.persistence.cassandra

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



class CassandraDAOSpec extends Specification with Injectable {

  implicit val injector = new UserModule

  "The 'Hello world' string" should {
    "contain 11 characters" in {
      "Hello world" must have size(11)
    }
    "start with 'Hello'" in {
      "Hello world" must startWith("Hello")
    }
    "end with 'world'" in {
      "Hello world" must endWith("world")
    }
 
  }
  
  "The CassandraDAO" should {
    
     "Test Category" in {
        val category = inject[Category]
        category.getAllCategories() must have size(5)
     }
 
     "Test OpenGameSession" in {
        val openGameSessionDao = inject[OpenGameSession]
        //val gameSession: OpenGameSessionModel = new OpenGameSessionModel("opengamesession_1")
        openGameSessionDao.addNewOpenGameSession("opengamesession_1")
        openGameSessionDao.addNewOpenGameSession("opengamesession_2")
        openGameSessionDao.addNewOpenGameSession("opengamesession_3")
        openGameSessionDao.addNewOpenGameSession("opengamesession_4")
        
        var gameSessions : List[OpenGameSessionModel] = openGameSessionDao.getGameSessions(10)
        gameSessions must have size(4)
        
        openGameSessionDao.removeOpenGameSession("opengamesession_1")
        openGameSessionDao.removeOpenGameSession("opengamesession_2")
        openGameSessionDao.removeOpenGameSession("opengamesession_3")
        openGameSessionDao.removeOpenGameSession("opengamesession_4")
        gameSessions = openGameSessionDao.getGameSessions(10)
        gameSessions must have size(0)
     }
     
     "Test GameSession" in {
        val gameSessionDao = inject[GameSession]
        gameSessionDao.addNewGameSession("gamesession_1", "minh_uid")
        val gameSession = gameSessionDao.getGameSessionById("gamesession_1")

        gameSession.gameSessionId equals "gamesession_1"
        gameSessionDao.updateGameSessionOnPlayer("gamesession_1", "minh_uid2", 1000001, true)
        gameSessionDao.updateGameSessionState("gamesession_1", 10)
        gameSessionDao.removeGameSession("gamesession_1")
     }
     
     "Test UserGameSession" in {
        val userGameSessionDao = inject[UserGameSession]
        userGameSessionDao.addNewUserGameSession("minh_uid", "gamesession_1")
        userGameSessionDao.addGameSessionIntoExistingRecord("minh_uid", "gamesession_2")
        userGameSessionDao.removeGameSession("minh_uid", "gamesession_1")
        val userGameSession : UserGameSessionModel = userGameSessionDao.getUserGameSessionsByUid("minh_uid")
        userGameSession.gameSessionIds must have size(1)

        true
     }
      
  }
}