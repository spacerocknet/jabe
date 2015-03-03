package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.GameModel
import play.Logger
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._

/**
 * Created by william on 1/13/15.
 */

trait GameInfo {
  def getGameInfoByGid(gid: Int): GameModel
  def getGameInfoByName(gName: String): GameModel
  def addGameInfo(game: GameModel): Boolean
  def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
                  bgp: Int): Boolean
  def updateGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
    bgp: Int): Boolean
  def updateGameInfo(game: GameModel): Boolean
  def getAllGames: List[GameModel]
  def close(): Unit
}

class GameInfoDAO (implicit inj: Injector) extends GameInfo with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
              = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  /**
   * Get game's information by game id
   * @param gid game id
   * @return game information (if success) or null (if not)
   */
  override def getGameInfoByGid(gid: Int): GameModel = {
    val ps: PreparedStatement = pStatements.get("GetGameInfoById").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val game: GameModel = new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
        row.getSet("categories", classOf[String]).toSet,
        row.getInt("battles_per_game"))
      game
    } else {
      return null
    }
  }

  /**
   * Get game's information by game name.
   * @param gName game name
   * @return game information (if success) or null (if not)
   */
  override def getGameInfoByName(gName: String): GameModel = {
    val ps: PreparedStatement = pStatements.get("GetGameInfoByName").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("game_name", gName)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val game: GameModel = new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
        row.getSet("categories", classOf[String]).toSet,
        row.getInt("battles_per_game"))
      game
    } else {
      null
    }
  }

  /**
   * Add new game to system.
   * @param game game model
   * @return true if success, otherwise false
   */
  override def addGameInfo(game: GameModel): Boolean = {
    addGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }
  override def addGameInfo(gid: Int, gameName: String, gameDescription: String, categories: Set[String],
                  bgp: Int): Boolean = {
    val ps: PreparedStatement = pStatements.get("AddGameInfo").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("gid", gid)
    bs.setString("game_name", gameName)
    bs.setString("game_description", gameDescription)
    bs.setSet("categories", categories)
    bs.setInt("battles_per_game", bgp)

    session.execute(bs)
    true
  }

  /**
   * Update game info
   * @param gId game id
   * @param gameName game name
   * @param gameDescription game description
   * @param categories game's categories
   * @param bgp battles per game
   * @return true if success, otherewise false
   */
  override def updateGameInfo(gId: Int, gameName: String, gameDescription: String, categories: Set[String],
                     bgp: Int): Boolean = {
    val ps: PreparedStatement = pStatements.get("UpdateGameInfo").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setSet("categories", categories)
    bs.setString("game_description", gameDescription)
    bs.setString("game_name", gameName)
    bs.setInt("battles_per_game", bgp)
    bs.setInt("gid", gId)
    session.execute(bs)

    true
  }

  /**
   * Update game info by game model
   * @param game game model
   * @return true if sucess, otherwise false
   */
  override def updateGameInfo(game: GameModel): Boolean = {
    updateGameInfo(game.gameId, game.gameName, game.gameDescription, game.categories, game.bpg)
  }

  /**
   * Get all game of the system
   * @return list of game models
   */
  override def getAllGames(): List[GameModel] = {
    val ps: PreparedStatement = pStatements.get("GetAllGames").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }

    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[GameModel] = scala.collection.mutable.ListBuffer()
    for (row <- result.all()) {
      if (row != null) {
        l.add(new GameModel(row.getInt("gid"), row.getString("game_name"), row.getString("game_description"),
          row.getSet("categories", classOf[String]).toSet,
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

    // Get all game
    ps = session.prepare("UPDATE spacerock.game_info SET categories = categories + ?, " +
      "game_description = ?, game_name = ?, battles_per_game = ? WHERE gid = ?;")
    pStatements.put("UpdateGameInfo", ps)
  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }
}
