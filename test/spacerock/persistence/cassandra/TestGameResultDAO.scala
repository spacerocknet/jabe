package spacerock.persistence.cassandra

import controllers.TestGlobal
import models.GameResultModel
import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
object TestGameResultDAO {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val gr: GameResult = new GameResultDAO
      val grm1: GameResultModel = new GameResultModel(1, 1, 1000, "uid 1")
      val grm2: GameResultModel = new GameResultModel(1, 1, 1000, "uid 2")
      val grm3: GameResultModel = new GameResultModel(1, 2, 1000, "uid 1")
      val grm4: GameResultModel = new GameResultModel(2, 2, 1000, "uid 1")
      val grm5: GameResultModel = new GameResultModel(2, 2, 1000, "uid 2")
      gr.addResults(grm1)
      gr.addResults(grm2)
      gr.addResults(grm3)
      gr.addResults(grm4)
      gr.addResults(grm5)
      gr.addResults("uid 5", 1, 1, 1000)

      println(gr.getResultsByGameLevel(1, 1))

      println(gr.getResultsByUid("uid 1"))
    }
  }
}
