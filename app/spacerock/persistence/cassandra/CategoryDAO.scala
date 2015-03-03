package spacerock.persistence.cassandra

import com.datastax.driver.core._
import models.CategoryModel
import play.Logger
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by william on 1/13/15.
 */

trait Category {
  def getCategoryByName(category: String): CategoryModel
  def addNewCategory(category: String, description: String): Boolean
  def updateCategory(category: String, gameId: Int, description: String): Boolean
  def getAllCategories(): List[CategoryModel]
  def close()
}

class CategoryDAO (implicit inj: Injector) extends Category with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")


  /**
   * Update category with game id list and description
   * @param category
   * @param gameId
   * @param description
   * @return true if update successfully, false otherwise
   */
  override def updateCategory(category: String, gameId: Int, description: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateCategory", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val set: mutable.HashSet[Int] = new mutable.HashSet[Int]
    set.add(gameId)
    bs.setString(0, description)
    bs.setSet(1, set)
    bs.setString(2, category)

    session.execute(bs)
    true
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
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)

    bs.setString("category", category)
    bs.setString("description", description)
    session.execute(bs)
    true
  }

  /**
   * Get category information by name. This will return an instance of found category
   * @param category category name
   * @return category model if exited or null if not found/error
   */
  override def getCategoryByName(category: String): CategoryModel = {
    val ps: PreparedStatement = pStatements.get("GetCategoryByName").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    val result: ResultSet = session.execute(bs)

    val row: Row = result.one()
    if (row != null) {
      val cat: CategoryModel = new CategoryModel(row.getString("category"),
        row.getString("description"),
        row.getSet( "game_list", classOf[Integer]).toList.map(i => i * 1))
      cat
    } else {
      null
    }
  }

  /**
   * Get all categories from system.
   * @return list of categories
   */
  override def getAllCategories(): List[CategoryModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllCategories", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[CategoryModel] = scala.collection.mutable.ListBuffer()
    for (r: Row <- result.all()) {
      l.add(new CategoryModel(r.getString("category"),
        r.getString("description"),
        r.getSet( "game_list", classOf[Integer]).toList.map(ii => ii * 1)))
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
    // update category
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.categories SET description = ?, " +
      "game_list = game_list + ? where category = ?;")
    pStatements.put("UpdateCategory", ps)

    // Add new category
    ps = session.prepare("INSERT INTO spacerock.categories (category, description) VALUES (?, ?) IF NOT EXIST;")
    pStatements.put("AddNewCategory", ps)

    // Get category info
    ps = session.prepare("SELECT * from spacerock.categories where category = ?;")
    pStatements.put("GetCategoryByName", ps)

    // Get all category
    ps = session.prepare("SELECT * FROM spacerock.categories ALLOW FILTERING;")
    pStatements.put("GetAllCategories", ps)
  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }
}
