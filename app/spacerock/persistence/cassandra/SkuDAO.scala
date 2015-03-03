package spacerock.persistence.cassandra

import java.util.Date

import models.SkuModel
import play.Logger
import scaldi.{Injector, Injectable}
import scala.collection.JavaConversions._
import com.datastax.driver.core._
/**
 * Created by william on 2/24/15.
 */
trait Sku {
  def addNewSku(skuId: Int, description: String, unitPrice: Float, startDate: Date,
                expiredTime: Date, extraData: String, discount: Float): Boolean
  def addNewSku(sku: SkuModel): Boolean
  def getSkuInfo(skuId: Int): SkuModel
  def close(): Unit
}

class SkuDAO (implicit inj: Injector) extends Sku with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

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
    // Insert new sku record
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.sku (sku_id, description, unit_price," +
      "start_time, expired_time, extra_data, discount) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?);")
    pStatements.put("AddNewSku", ps)

    // get sku info
    ps = session.prepare("SELECT * FROM spacerock.sku WHERE sku_id = ?;")
    pStatements.put("GetSkuInfo", ps)

//      // get sku by start time
//      ps = session.prepare("SELECT * from spacerock.sku WHERE start_time <= ? ALLOW FILTERING;")
//      pStatements.put("GetSkusByStartTime", ps)
//
//      // get sku by expired time
//      ps = session.prepare("SELECT * from spacerock.sku WHERE expired_time <= ? ALLOW FILTERING;")
//      pStatements.put("GetSkusByExpiredTime", ps)
  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }

/**
 * Add new Sku
 * @param skuId sku id
 * @param description sku description
 * @param unitPrice sku's unit price
 * @param startTime start time of sku
 * @param expiredTime expired time of sku
 * @param extraData extra data
 * @param discount discount information
 * @return true if success, otherwise false
 */
  override def addNewSku(skuId: Int, description: String, unitPrice: Float, startTime: Date,
                          expiredTime: Date, extraData: String, discount: Float): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewSku", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("sku_id", skuId)
    bs.setString("description", description)
    bs.setFloat("unit_price", unitPrice)
    bs.setDate("start_time", startTime)
    bs.setDate("expired_time", expiredTime)
    bs.setString("extra_data", extraData)
    bs.setFloat("discount", discount)

    session.execute(bs)

    true
  }

/**
 * Add new sku with sku model
 * @param sku sku model
 * @return true if success, otherwise false
 */
  override def addNewSku(sku: SkuModel): Boolean = {
    addNewSku(sku.skuId, sku.description, sku.unitPrice, sku.startTime, sku.expiredTime,
      sku.extraData, sku.discount)
  }

/**
 * Get sku info by sku id
 * @param skuId sku id
 * @return sku model if success, otherwise false
 */
  override def getSkuInfo(skuId: Int): SkuModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetSkuInfo", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("sku_id", skuId)
    val result: ResultSet = session.execute(bs)
    val r: Row = result.one()
    if (r != null) {
      val sku: SkuModel = new SkuModel(r.getInt("sku_id"),
                                       r.getString("description"),
                                       r.getFloat("unit_price"),
                                       r.getDate("start_time"),
                                       r.getDate("expired_time"),
                                       r.getString("extra_data"),
                                       r.getFloat("discount"))
      sku
    } else {
      null
    }
  }
}
