package spacerock.persistence.cassandra


import models.GameModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

import scala.collection.mutable

/**
 * Created by william on 3/1/15.
 */
object TestGameInfoDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val gi: GameInfo = inject [GameInfo]
      // GameModel(gameId: Int, gameName: String, gameDescription: String,
      // categories: List[String], bpg: Int)
      val l: mutable.Set[String] = new mutable.HashSet[String]
      l.add("cat 1")
      l.add("cat 2")
      val g1: GameModel = new GameModel(1, "game 1", "description for game 1", l.toSet, 10)
      assert(gi.addGameInfo(g1))

      l.clear()
      l.add("cat 3")
      l.add("cat 5")
      val g2: GameModel = new GameModel(2, "game 2", "description for game 2", l.toSet, 110)
      assert(gi.addGameInfo(g2))

      l.clear()
      l.add("cat 3")
      l.add("cat 1")
      l.add("cat 7")
      assert(gi.addGameInfo(3, "game 3", "description for game 3", l.toSet, 110))

      println(gi.getAllGames)

      println(gi.getGameInfoByGid(1))

      println(gi.getGameInfoByName("game 2"))

      val g3: GameModel = new GameModel(2, "game 2", "description for game 2 (updated)", g2.categories, 111)
      val g10: GameModel = new GameModel(10, "game 2", "description for game 10 (updated)", g2.categories, 111)
      assert(gi.updateGameInfo(g3))
      assert(gi.addGameInfo(g10))

      println(gi.getGameInfoByName("game 2"))

      println(gi.getGameInfoByGid(2))

    }
  }
}
