package spacerock.persistence

import com.datastax.driver.core._
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._
/**
 * Created by william on 1/25/15.
 */

trait GameResult {
  def addGameResults(uid: String, gameResult: List[String]): Boolean
  def getGameResults(uid: String): List[String]
  def addMoreGameResults(uid: String, gameResult: List[String]): Boolean
}
class GameResultDAO  (implicit inj: Injector) extends GameResult with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  def addGameResults(uid: String, gameResults: List[String]): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddGameResults", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setList("results", gameResults)
    session.execute(bs)
    true
  }
  def getGameResults(uid: String): List[String] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetResultsByUid", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(1, uid)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer()
    for (row <- result.all()) {
      if (row != null) {
        for (s <- row.getList("results", classOf[String]))
          l.add(s)
      }
    }
    l.toList
  }
  def addMoreGameResults(uid: String, gameResults: List[String]): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddMoreResults", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setList(0, gameResults)
    bs.setString(1, uid)
    session.execute(bs)
    true
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
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.game_result (uid, results) " +
      "VALUES (?, ?);")
    pStatements.put("AddGameResults", ps)

    // Add more results
    ps = session.prepare("UPDATE spacerock.game_result SET results = results + ? " +
      "WHERE uid = ?;")
    pStatements.put("AddMoreResults", ps)

    // Get game result by uid
    ps = session.prepare("SELECT results from spacerock.game_result where uid = ?;")
    pStatements.put("GetResultsByUid", ps)
  }

  def close() = {
    if (cluster != null)
      cluster.close()
  }
}
