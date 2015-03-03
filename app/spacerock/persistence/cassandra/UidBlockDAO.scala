package spacerock.persistence.cassandra

import com.datastax.driver.core._
import play.Logger
import scaldi.{Injector, Injectable}
import scala.collection.JavaConversions._

/**
 * Created by william on 2/24/15.
 */

trait UidBlock {
  def addNewBlock(blockId: Int, blocks: Set[String], status: Boolean, grantedServer: Int): Boolean
  def assignBlockToServer(blockId: Int, serverId: Int): Set[String]
  def freeBlock(blockId: Int): Boolean
  def updateStatus(blockId: Int, status: Boolean): Boolean
  def getNextBlockId(): Int
  def close(): Unit
}

class UidBlockDAO (implicit  inj: Injector) extends UidBlock with Injectable {
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
    // Insert new block
    var ps: PreparedStatement = session.prepare("INSERT INTO spacerock.uid_blocks (block_id, granted_server, status, ids) " +
      "VALUES (?, ?, ?, ?);")
    pStatements.put("AddNewBlock", ps)

    // assign a block to a server.
    ps = session.prepare("UPDATE spacerock.uid_blocks SET granted_server = ?, status = ? WHERE block_id = ?;")
    pStatements.put("AssignBlock", ps)

    // update status
    ps = session.prepare("UPDATE spacerock.uid_blocks SET status = ? WHERE block_id = ?;")
    pStatements.put("UpdateStatus", ps)

    // get block
    ps = session.prepare("SELECT ids from spacerock.uid_blocks WHERE block_id = ?;")
    pStatements.put("GetBlockData", ps)

    // get all (un)managed  block ids
    ps = session.prepare("SELECT block_id from spacerock.uid_blocks WHERE status = ? LIMIT 1;")
    pStatements.put("GetBlockIdsWithStatus", ps)

  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }

  /**
   * Add new uid block
   * @param blockId block id
   * @param blocks set of uid
   * @param status status (granted or not)
   * @param grantedServer granted server
   * @return true if success, otherwise false
   */
  override def addNewBlock(blockId: Int, blocks: Set[String], status: Boolean, grantedServer: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewBlock", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("block_id", blockId)
    bs.setInt("granted_server", grantedServer)
    bs.setBool("status", status)
    bs.setSet("ids", blocks)

    val result: ResultSet = session.execute(bs)
    val r: Row = result.one()
    if (r != null) {
      r.getBool(0)
    } else {
      false
    }
  }

  /**
   * Update status of uid block. This method will be called when a server successfully grants the uid block with @blockId
   * @param blockId block id
   * @param status status
   * @return true if success, otherwise false
   */
  override def updateStatus(blockId: Int, status: Boolean): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateStatus", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("status", status)
    bs.setInt("block_id", blockId)
    session.execute(bs)

    true
  }

  /**
   * Assign block to server.
   * @param blockId block id
   * @param serverId server id
   * @return true if success, otherwise false
   */
  override def assignBlockToServer(blockId: Int, serverId: Int): Set[String] = {
    var ps: PreparedStatement = pStatements.getOrElse("AssignBlock", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    var bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("granted_server", serverId)
    bs.setBool("status", true)
    bs.setInt("block_id", blockId)
    session.execute(bs)

    // get data
    ps = pStatements.getOrElse("GetBlockData", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return null
    }
    bs = new BoundStatement(ps)
    bs.setInt("block_id", blockId)
    val result: ResultSet = session.execute(bs)
    for (r: Row <- result.all()) {
      return r.getSet("ids", classOf[String]).toSet
    }
      return null
  }

  /**
   * Get next free block
   * @return block id >= 0 if success, otherwise -1
   */
  override def getNextBlockId(): Int = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBlockIdsWithStatus", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return -1
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("status", false)
    val result: ResultSet = session.execute(bs)
    for (r: Row <- result.all()) {
      return r.getInt("block_id")
    }
    -1
  }

  /**
   * Free a block with @blockId
   * @param blockId block id
   * @return true if success, otherwise false
   */
  override def freeBlock(blockId: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AssignBlock", null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("granted_server", -1)
    bs.setBool("status", false)
    bs.setInt("block_id", blockId)
    session.execute(bs)

    true
  }
}
