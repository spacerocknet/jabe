package spacerock.persistence.cassandra

import models.GameModel

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
/**
 * Created by william on 3/1/15.
 */
object TestGameInfoDAO {
//  def main(args: Array[String]): Unit = {
//    val gi: GameInfo = new GameInfoDAO()
//
//    // GameModel(gameId: Int, gameName: String, gameDescription: String,
//    // categories: List[String], bpg: Int)
//    var l: ListBuffer[String] = new ListBuffer[String]
//    l.add("cat 1")
//    l.add("cat 2")
//    val g1: GameModel = new GameModel(1, "game 1", "description for game 1", l.toList, 10)
//    gi.addGameInfo(g1)
//    l.clear()
//    l.add("cat 3")
//    l.add("cat 5")
//    val g2: GameModel = new GameModel(2, "game 2", "description for game 2", l.toList, 110)
//    gi.addGameInfo(g2)
//    l.clear()
//    l.add("cat 3")
//    l.add("cat 1")
//    l.add("cat 7")
//    gi.addGameInfo(3, "game 3", "description for game 3", l.toList, 110)
//
//    println(gi.getAllGames())
//
//    println(gi.getGameInfoByGid(1))
//
//    println(gi.getGameInfoByName("game 2"))
//
//    val g3: GameModel = new GameModel(2, "game 2", "description for game 2 (updated)", g2.categories, 111)
//
//    gi.updateGameInfo(g3)
//
//    println(gi.getGameInfoByGid(2))
//
//    gi.close()
//  }
}