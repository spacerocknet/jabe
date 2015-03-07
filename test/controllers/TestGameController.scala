package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Created by william on 3/5/15.
 */

object TestGameController extends Specification {

  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(TestGlobal))
    running(app) {
      val gameName = "game 2"
      val description = "description 2"
      val categories: List[String] = List("cat 10", "cat 11")
      val bpg = 1
      val uid: String = "jdaskfjewkrklvafdsfd"
      val uid2: String = "u943214jfkjdsla"
      val gameId = testAddNewGameInfo(gameName, description, categories, bpg)
      testGetGameInfoById(gameId)
      testGetGameInfoById(1000)
      testGetGameInfoByName("game 1")
      testGetGameInfoByName("game 2")
      testGetGameInfoByName("game 100")

      testSaveGameResult(uid, gameId, 1, 10000)
      testSaveGameResult(uid, gameId, 2, 20000)
      testSaveGameResult(uid2, 9, 3, 10000)
      testSaveGameResult(uid2, 9, 3, 20000)

      testGetGameResultByUid(uid)
      testGetGameResultByUid(uid2)

      testGetGameResultByUidGame(uid, gameId)
      testGetGameResultByUidGame(uid, 5)

      testGetGameResultByUidGame(uid2, gameId)
      testGetGameResultByUidGame(uid2, 9)

    }
  }

  def testAddNewGameInfo(gameName: String, description: String, categories: List[String], bpg: Int): Int = {
    val response = route(FakeRequest(POST, "/v1/game/add")
      .withJsonBody(Json.obj("game-name" -> gameName,
                             "description" -> description,
                             "categories" -> Json.toJson(categories),
                             "battles-per-game" -> bpg))).get
    println(contentAsString(response))
    val gameId: Int = (contentAsJson(response) \ "game-id").asOpt[Int].getOrElse(-1)
    gameId
  }

  def testGetGameInfoById(gameId: Int): Unit = {
    val response = route(FakeRequest(GET, "/v1/game/%d" format gameId)).get
    println(contentAsString(response))
  }

  def testGetGameInfoByName(gname: String): Unit = {
    val response = route(FakeRequest(POST, "/v1/game/byname")
    .withJsonBody(Json.obj("game-name" -> gname))).get
    println(contentAsString(response))
  }

  def testSaveGameResult(uid: String, gameId: Int, level: Int, score: Long): Unit = {

    // valid
    val response = route(FakeRequest(POST, "/v1/game/saveresult")
      .withJsonBody(Json.obj("uid" -> uid,
                             "game-id" -> gameId,
                             "level" -> level,
                             "score" -> score))).get
    println(contentAsString(response))
  }

  def testGetGameResultByUid(uid: String): Unit = {
    val response = route(FakeRequest(GET, "/v1/game/getresult/%s" format uid)).get
    println(contentAsString(response))
  }

  def testGetGameResultByUidGame(uid: String, gameId: Int): Unit = {
    val response = route(FakeRequest(POST, "/v1/game/getresult")
      .withJsonBody(Json.obj("uid" -> uid,
                             "game-id" -> gameId))).get
    println(contentAsString(response))
  }
}

