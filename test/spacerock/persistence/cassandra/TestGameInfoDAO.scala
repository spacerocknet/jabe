package spacerock.persistence.cassandra

import controllers.TestGlobal
import models.GameModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
/**
 * Created by william on 3/1/15.
 */
object TestGameInfoDAO {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val gi: GameInfo = new GameInfoDAO()
      // GameModel(gameId: Int, gameName: String, gameDescription: String,
      // categories: List[String], bpg: Int)
      val l: Set[String] = new HashSet[String]
      l.add("cat 1")
      l.add("cat 2")
      val g1: GameModel = new GameModel(1, "game 1", "description for game 1", l.toSet, 10)
      gi.addGameInfo(g1)
      l.clear()
      l.add("cat 3")
      l.add("cat 5")
      val g2: GameModel = new GameModel(2, "game 2", "description for game 2", l.toSet, 110)
      gi.addGameInfo(g2)
      l.clear()
      l.add("cat 3")
      l.add("cat 1")
      l.add("cat 7")
      gi.addGameInfo(3, "game 3", "description for game 3", l.toSet, 110)

      println(gi.getAllGames)

      println(gi.getGameInfoByGid(1))

      println(gi.getGameInfoByName("game 2"))

      val g3: GameModel = new GameModel(2, "game 2", "description for game 2 (updated)", g2.categories, 111)

      gi.updateGameInfo(g3)

      println(gi.getGameInfoByGid(2))

    }
  }
}
