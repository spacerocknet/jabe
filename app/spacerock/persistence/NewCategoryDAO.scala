package spacerock.persistence

import com.datastax.driver.core._
import models.Category
import scaldi.{Injectable, Injector}
import scala.collection.JavaConversions._

/**
 * Created by william on 1/13/15.
 */

trait NewCategory {
  def getCategoryByName(category: String): Category
  def addNewCategory(category: String, battlesPerGame: Int): Boolean
  def updateCategory(category: String, battlesPerGame: Int): Boolean
  def getAllCategories(): List[Category]
}

class NewCategoryDAO (implicit inj: Injector) extends NewCategory with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  override def updateCategory(category: String, battlesPerGame: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateCategory", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("battles_per_game", battlesPerGame)
    bs.setString("category", category)

    session.execute(bs)
    true
  }

  override def addNewCategory(category: String, battlesPerGame: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewCategory", null)
    val bs: BoundStatement = new BoundStatement(ps)
    val time: Long = System.currentTimeMillis()

    bs.setString("user_name", category)
    bs.setInt("battles_per_game", battlesPerGame)
    session.execute(bs)
    true
  }

  override def getCategoryByName(category: String): Category = {
    val ps: PreparedStatement = pStatements.get("GetCategoryByName").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("category", category)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    val cat: Category = new Category(row.getString("category"), row.getInt("battles_per_game"))
    cat
  }

  override def getAllCategories(): List[Category] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllCategories", null)
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[Category] = scala.collection.mutable.ListBuffer()
    for (r: Row <- result.all()) {
      l.add(new Category(r.getString("category"), r.getInt("battles_per_game")))
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
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.categories SET battles_per_game = ? where category = ?;")
    pStatements.put("UpdateCategory", ps)

    // Add new category
    ps = session.prepare("INSERT INTO spacerock.categories (category, battles_per_game) VALUES (?, ?);")
    pStatements.put("AddNewCategory", ps)

    // Get user info
    ps = session.prepare("SELECT * from spacerock.categories where category = ?;")
    pStatements.put("GetCategoryByName", ps)

    // Get all users
    ps = session.prepare("SELECT * FROM spacerock.categories ALLOW FILTERING;")
    pStatements.put("GetAllCategories", ps)
  }

  def close() = {
    if (cluster != null)
      cluster.close()
  }
}
