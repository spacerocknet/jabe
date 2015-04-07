package spacerock.persistence.cassandra

import models.GameResultModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
object TestGameResultDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val gr: GameResult = inject [GameResult]
      val grm1: GameResultModel = new GameResultModel(1, 1, 1000, "uid 1")
      val grm2: GameResultModel = new GameResultModel(1, 1, 1000, "uid 2")
      val grm3: GameResultModel = new GameResultModel(1, 2, 1000, "uid 1")
      val grm4: GameResultModel = new GameResultModel(2, 2, 1000, "uid 1")
      val grm5: GameResultModel = new GameResultModel(2, 2, 1000, "uid 2")
      assert(gr.addResults(grm1))
      assert(gr.addResults(grm2))
      assert(gr.addResults(grm3))
      assert(gr.addResults(grm4))
      assert(gr.addResults(grm5))
      assert(gr.addResults("uid 5", 1, 1, 1000))
      println("Get result by game level")
      println(gr.getResultsByGameLevel(1, 1))
      println("Get result by uid of game")
      println(gr.getResultsByUidOfGame("uid 1", 1))
      println("Get result by uid")
      println(gr.getResultsByUid("uid 1"))
    }
  }
}
