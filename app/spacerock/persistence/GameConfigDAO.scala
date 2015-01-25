package spacerock.persistence

import java.util.UUID

import com.datastax.driver.core._
import models.{Game}
import scaldi.{Injectable, Injector}
import scala.collection.JavaConversions._

/**
 * Created by william on 1/13/15.
 */

trait GameConfig {
  def getGameInfoByGid(gid: Int): Game
  def getGameInfoByName(gName: String): Game
  def addGameInfo(game: Game): Boolean
  def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: List[String],
                  bgp: Int): Boolean
  def updateGameInfo(gid: Int, gameName: String, gameDescription: String, categories: List[String],
    bgp: Int): Boolean
  def updateGameInfo(game: Game): Boolean
  def getAllGames(): List[Game]
}

class GameConfigDAO (implicit inj: Injector) extends GameConfig with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
              = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  def getGameInfoByGid(gid: Int): Game = {
    val ps: PreparedStatement = pStatements.get("GetQuizByQid").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val game: Game = new Game(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
        row.getList("categories", classOf[String]).toList,
        row.getInt("battles_per_game"))
      game
    } else {
      null
    }
  }
  def getGameInfoByName(gName: String): Game = {
    val ps: PreparedStatement = pStatements.get("GetQuizByQid").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("game_name", gName)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val game: Game = new Game(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
        row.getList("categories", classOf[String]).toList,
        row.getInt("battles_per_game"))
      game
    } else {
      null
    }
  }
  def addGameInfo(game: Game): Boolean = {
    addGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }
  def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: List[String],
                  bgp: Int): Boolean = {
    val ps: PreparedStatement = pStatements.get("AddGameInfo").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    bs.setString("game_name", gameName)
    bs.setString("game_description", gameDescription)
    bs.setList("categories", categories)
    bs.setInt("battles_per_game", bgp)

    session.execute(bs)
    true
  }
  def updateGameInfo(gId: Int, gameName: String, gameDescription: String, categories: List[String],
                     bgp: Int): Boolean = ???
  def updateGameInfo(game: Game): Boolean = {
    updateGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }

  def getAllGames(): List[Game] = {
    val ps: PreparedStatement = pStatements.get("GetAllGames").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[Game] = scala.collection.mutable.ListBuffer()
    for (row <- result.all()) {
      if (row != null) {
        l.add(new Game(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
          row.getList("categories", classOf[String]).toList,
          row.getInt("battles_per_game")))
      }
    }
    l.toList
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
    // get game info by id
    var ps: PreparedStatement = session.prepare("SELECT * from spacerock.game_info where gid = ?;")
    pStatements.put("GetGameInfoById", ps)

    // insert new game
    ps = session.prepare("INSERT INTO spacerock.game_info (gid, game_name, game_description, categories, battles_per_game) " +
      "VALUES (?, ?, ?, ?, ?);")
    pStatements.put("AddGameInfo", ps)

    // get game info by game name
    ps = session.prepare("SELECT * from spacerock.game_info where game_name = ? ALLOW FILTERING;")
    pStatements.put("GetGameInfoByName", ps)

    // Get all game
    ps = session.prepare("SELECT * FROM spacerock.game_info ALLOW FILTERING;")
    pStatements.put("GetAllGames", ps)
  }

  def close() = {
    if (cluster != null)
      cluster.close()
  }
}
