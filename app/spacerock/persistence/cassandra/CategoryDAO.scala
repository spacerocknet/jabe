package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.CategoryModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable

/**
 * Created by william on 1/13/15.
 */

trait Category {
  def getCategoryByName(category: String): CategoryModel
  def addNewCategory(category: String, description: String): Boolean
  def updateCategory(category: String, gameId: Int, description: String): Boolean
  def addNewGames(category: String, gameIds: Set[Int]): Boolean
  def addNewGame(category: String, gameId: Int): Boolean
  def getAllCategories(): List[CategoryModel]
  def getAllCategoriesByGameId(gid : Integer): List[CategoryModel]
  def lastError: Int
}

class CategoryDAO (implicit inj: Injector) extends Category with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()
  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  /**
   * Add new games to game list of category
   * @param category category name
   * @param gameIds game id list
   * @return true if success, false otherwise
   */
  override def addNewGames(category: String, gameIds: Set[Int]): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddGameIdsToCategory", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("category", category)
    bs.setSet("game_list", gameIds)

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }

  /**
   * Add new game to game list of category
   * @param category category name
   * @param gameId game id
   * @return true if success, false otherwise
   */
  override def addNewGame(category: String, gameId: Int): Boolean = {
    val gameIds: Set[Int] = HashSet(gameId)
    addNewGames(category, gameIds)
  }

  /**
   * Update category with game id list and description
   * @param category
   * @param gameId
   * @param description
   * @return true if update successfully, false otherwise
   */
  override def updateCategory(category: String, gameId: Int, description: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateCategory", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val set: mutable.HashSet[Int] = new mutable.HashSet[Int]
    set.add(gameId)
    bs.setString(0, description)
    bs.setSet(1, set)
    bs.setString(2, category)

    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }

  /**
   * Add new category. In the first phase, admin may insert a category first,
   * then he inserts game info latter.
   * So we don't need to insert game_id field right now. If the category existed, this will not be inserted
   * @param category
   * @param description
   * @return true if add successfully, false otherwise
   */
  override def addNewCategory(category: String, description: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewCategory", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)

    bs.setString("category", category)
    bs.setString("description", description)
    if (sessionManager.execute(bs) == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }

  /**
   * Get category information by name. This will return an instance of found category
   * @param category category name
   * @return category model if exited or null if not found/error
   */
  override def getCategoryByName(category: String): CategoryModel = {
    val ps: PreparedStatement = pStatements.get("GetCategoryByName").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        val cat: CategoryModel = new CategoryModel(row.getString("category"),
          row.getString("description"),
          row.getSet("game_list", classOf[Integer]).toList.map(i => i * 1))
        cat
      } else {
        null
      }
    }
  }

  /**
   * Get all categories from system.
   * @return list of categories
   */
  override def getAllCategories(): List[CategoryModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllCategories", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      val l: scala.collection.mutable.ListBuffer[CategoryModel] = scala.collection.mutable.ListBuffer()
      for (r: Row <- result.all()) {
        if (r != null) {
          l.add(new CategoryModel(r.getString("category"),
            r.getString("description"),
            r.getSet("game_list", classOf[Integer]).toList.map(ii => ii.asInstanceOf[Int] )))
        }
      }
      l.toList
    }
  }
  
    /**
   * Get all categories from system for a specified game id
   * @return list of categories
   */
   def getAllCategoriesByGameId(gid : Int): List[CategoryModel] = {
    //TODOs: filtering out game id from the results
    val ps: PreparedStatement = pStatements.getOrElse("GetAllCategories", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      val l: scala.collection.mutable.ListBuffer[CategoryModel] = scala.collection.mutable.ListBuffer()
      for (r: Row <- result.all()) {
        if (r != null) {
          l.add(new CategoryModel(r.getString("category"),
                                  r.getString("description"))
        }
      }
      l.toList
    }
  }


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // update category
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.categories SET description = ?, " +
      "game_list = game_list + ? where category = ?;")
    if (ps != null)
      pStatements.put("UpdateCategory", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.categories SET " +
      "game_list = game_list + ? where category = ?;")
    if (ps != null)
      pStatements.put("AddGameIdsToCategory", ps)
    else
      _lastError = sessionManager.lastError

    // Add new category
    ps = sessionManager.prepare("INSERT INTO spacerock.categories (category, description) VALUES (?, ?) IF NOT EXISTS;")
    if (ps != null)
      pStatements.put("AddNewCategory", ps)
    else
      _lastError = sessionManager.lastError

    // Get category info
    ps = sessionManager.prepare("SELECT * from spacerock.categories where category = ?;")
    if (ps != null)
      pStatements.put("GetCategoryByName", ps)
    else
      _lastError = sessionManager.lastError

    // Get all category
    ps = sessionManager.prepare("SELECT * FROM spacerock.categories ALLOW FILTERING;")
    if (ps != null)
      pStatements.put("GetAllCategories", ps)
    else
      _lastError = sessionManager.lastError
  }

}
