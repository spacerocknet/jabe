package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.GameResultModel
import play.Logger
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 1/23/15.
 */

trait GameResult {
  def addResults(uid: String, gid: Int, level: Int, score: Long): Boolean
  def addResults(grm: GameResultModel): Boolean
  def getResultsByUid(uid: String): List[GameResultModel]
  def getResultsByUidOfGame(uid: String, gameId: Int): Map[Int, Long]
  def getResultsByGameLevel(gameId: Int, level: Int): Map[String, Long]
  def close(): Unit
}

class GameResultDAO (implicit inj: Injector) extends GameResult with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  /**
   * Add new results from what user played
   * @param uid user id
   * @param gid game id that user played
   * @param level game level that user played
   * @param score score of playing game level
   * @return true if success, otherwise false
   */
  override def addResults(uid: String, gid: Int, level: Int, score: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddResults", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gid)
    bs.setInt("level", level)
    bs.setLong("score", score)
    session.execute(bs)
    true
  }

  /**
   * Add new game by game result model
   * @param grm game result model
   * @return true if success, otherwise false
   */
  override def addResults(grm: GameResultModel): Boolean = {
    addResults(grm.uid, grm.gid, grm.level, grm.score)
  }

  /**
   * get all results of all game level that user played by user id
   * @param uid
   * @return list of game result model
   */
  def getResultsByUid(uid: String): List[GameResultModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultsByUid", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, uid)
    val result: ResultSet = session.execute(bs)
    val l: ListBuffer[GameResultModel] = new ListBuffer[GameResultModel]
    for (row <- result.all()) {
      if (row != null) {
        l.add(new GameResultModel(row.getInt("game_id"),
        row.getInt("level"),
        row.getLong("score"),
        uid))
      }
    }
    l.toList
  }

  /**
   * Get all results by game level. This method will be used in case of select top user of game and level
   * @param gameId game id
   * @param level game level
   * @return map of user id and game level score
   */
  override def getResultsByGameLevel(gameId: Int, level: Int): Map[String, Long] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultByGameLevel", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val map: scala.collection.mutable.HashMap[String, Long] = new scala.collection.mutable.HashMap[String, Long]
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("game_id", gameId)
    bs.setInt("level", level)

    val result: ResultSet = session.execute(bs)
    for (row: Row <- result.all()) {
      if (row != null) {
        map.put(row.getString("uid"), row.getLong("score"))
      }
    }
    map.toMap
  }

  /**
   * Get all results by uid and game id of all time. It will return a map of level by score
   * @param uid user id
   * @param gameId game id
   * @return map of level and score
   */
  override def getResultsByUidOfGame(uid: String, gameId: Int): Map[Int, Long] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultByUidGame", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val map: scala.collection.mutable.HashMap[Int, Long] = new scala.collection.mutable.HashMap[Int, Long]
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("game_id", gameId)
    bs.setString("uid", uid)

    val result: ResultSet = session.execute(bs)
    for (row: Row <- result.all()) {
      if (row != null) {
        map.put(row.getInt("level"), row.getLong("score"))
      }
    }
    map.toMap
  }

  def connect(node: String): Boolean = {
    cluster = Cluster.builder().addContactPoint(node).build()
    val metadata = cluster.getMetadata()
    var countHost: Int = 0
    metadata.getAllHosts() map {
      case host => countHost += 1
    }
    session = cluster.connect()

    if (countHost < 1)
      false
    else {
      init()
      true
    }
  }


  def init() = {
    // Add Game result
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.game_result SET score = ? " +
      "WHERE game_id = ? AND level = ? and uid = ?;")
    pStatements.put("AddResults", ps)

    // Get game result by uid
    ps = session.prepare("SELECT * from spacerock.game_result where uid = ?;")
    pStatements.put("GetResultsByUid", ps)

    // Get game result by game id and level;
    ps = session.prepare("SELECT score, uid from spacerock.game_result where game_id = ? and level = ?;")
    pStatements.put("GetResultByGameLevel", ps)

    // Get game result by user id and game id;
    ps = session.prepare("SELECT score, level from spacerock.game_result where game_id = ? and uid = ?;")
    pStatements.put("GetResultByUidGame", ps)

  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }


}
