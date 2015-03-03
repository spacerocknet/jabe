package spacerock.persistence.cassandra

import java.util.Date

import com.datastax.driver.core._
import models.BillingRecordModel
import play.Logger
import scaldi.{Injector, Injectable}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Created by william on 2/23/15.
 */

trait Billing {
  def addNewBill(uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, totalDiscount: Float): Boolean
  def addNewBill(br: BillingRecordModel): Boolean
  def getAllBillsOfUser(uid: String): List[BillingRecordModel]
  def getAllBillsOfUserWithDate(uid: String, from: Date, to: Date): List[BillingRecordModel]
  def getBillsOfUserFromGame(uid: String, gameId: Int): List[BillingRecordModel]
  def getBillsOfUserFromGameWithDate(uid: String, gameId: Int, from: Date, to: Date): List[BillingRecordModel]
  def close(): Unit
}

class BillingDAO (implicit inj: Injector) extends Billing with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  def BillingDAO() = {}

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
    // Insert new bill
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.billing (uid, ts, game_id, sku_id, " +
      "n_items, discount) " +
      "VALUES (?, ?, ?, ?, ?, ?);")
    pStatements.put("AddNewBill", ps)

    // Get all billing record of a user
    ps = session.prepare("SELECT * FROM spacerock.billing WHERE uid = ?;")
    pStatements.put("GetBills", ps)

    // Get all billing record of a user in a rage of date
    ps = session.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND ts > ? and ts < ?;")
    pStatements.put("GetBillsWithBoundDate", ps)

    // get all billing records of a user in a game
    ps = session.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND game_id = ?;")
    pStatements.put("GetBillsFromGame", ps)

    // get all billing records of a user in a game
    ps = session.prepare("SELECT * FROM spacerock.billing WHERE uid = ? AND game_id = ? and ts < ? and ts > ?;")
    pStatements.put("GetBillsFromGameWithDate", ps)
  }

  override def close(): Unit = {
    if (cluster != null)
      cluster.close()
  }

  override def addNewBill(uid: String, ts: Date, gameId: Int, skuId: Int, nItems: Int, discount: Float): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewBill", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setDate("ts", ts)
    bs.setInt("game_id", gameId)
    bs.setInt("sku_id", skuId)
    bs.setInt("n_items", nItems)
    bs.setFloat("discount", discount)

    session.execute(bs)

    true
  }

  override def addNewBill(br: BillingRecordModel): Boolean = {
    addNewBill(br.uid, br.ts, br.gameId, br.skuId, br.nItems, br.discount)
  }

  override def getAllBillsOfUser(uid: String): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBills", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    val result: ResultSet = session.execute(bs)

    val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
    for (r: Row <- result.all()) {
      if (r != null) {
        val sku: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
          r.getDate("ts"),
          r.getInt("game_id"),
          r.getInt("sku_id"),
          r.getInt("n_items"),
          r.getFloat("discount"))
        l.add(sku)
      }
    }
    l.toList
  }

  override def getAllBillsOfUserWithDate(uid: String, from: Date, to: Date): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsWithBoundDate", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setDate(1, from)
    bs.setDate(2, to)
    val result: ResultSet = session.execute(bs)

    val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
    for (r: Row <- result.all()) {
      if (r != null) {
        val sku: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
          r.getDate("ts"),
          r.getInt("game_id"),
          r.getInt("sku_id"),
          r.getInt("n_items"),
          r.getFloat("discount"))
        l.add(sku)
      }
    }
    l.toList
  }

  override def getBillsOfUserFromGame(uid: String, gameId: Int): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsFromGame", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gameId)
    val result: ResultSet = session.execute(bs)
    val r: Row = result.one()
    val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
    if (r != null) {
      val br: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
        r.getDate("ts"),
        r.getInt("game_id"),
        r.getInt("sku_id"),
        r.getInt("n_items"),
        r.getFloat("discount"))
      l.add(br)
    }
    l.toList
  }

  override def getBillsOfUserFromGameWithDate(uid: String, gameId: Int,
                                              from: Date, to: Date): List[BillingRecordModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBillsFromGameWithDate", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("game_id", gameId)
    bs.setDate(2, from)
    bs.setDate(3, to)
    val result: ResultSet = session.execute(bs)
    val r: Row = result.one()
    val l: ListBuffer[BillingRecordModel] = new ListBuffer[BillingRecordModel]
    if (r != null) {
      val br: BillingRecordModel = new BillingRecordModel(r.getString("uid"),
        r.getDate("ts"),
        r.getInt("game_id"),
        r.getInt("sku_id"),
        r.getInt("n_items"),
        r.getFloat("discount"))
      l.add(br)
    }
    l.toList
  }

}
