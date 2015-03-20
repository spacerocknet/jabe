package spacerock.persistence.cassandra

import com.datastax.driver.core._
import play.Logger
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants
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
  def lastError: Int
}

class UidBlockDAO (implicit  inj: Injector) extends UidBlock with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Insert new block
    var ps: PreparedStatement = sessionManager.prepare("INSERT INTO spacerock.uid_blocks (block_id, granted_server, status, ids) " +
      "VALUES (?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddNewBlock", ps)
    else
      _lastError = sessionManager.lastError

    // assign a block to a server.
    ps = sessionManager.prepare("UPDATE spacerock.uid_blocks SET granted_server = ?, status = ? WHERE block_id = ?;")
    if (ps != null)
      pStatements.put("AssignBlock", ps)
    else
      _lastError = sessionManager.lastError

    // update status
    ps = sessionManager.prepare("UPDATE spacerock.uid_blocks SET status = ? WHERE block_id = ?;")
    if (ps != null)
      pStatements.put("UpdateStatus", ps)
    else
      _lastError = sessionManager.lastError

    // get block
    ps = sessionManager.prepare("SELECT ids from spacerock.uid_blocks WHERE block_id = ?;")
    if (ps != null)
      pStatements.put("GetBlockData", ps)
    else
      _lastError = sessionManager.lastError

    // get all (un)managed  block ids
    ps = sessionManager.prepare("SELECT block_id from spacerock.uid_blocks WHERE status = ? LIMIT 1;")
    if (ps != null)
      pStatements.put("GetBlockIdsWithStatus", ps)
    else
      _lastError = sessionManager.lastError

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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("block_id", blockId)
    bs.setInt("granted_server", grantedServer)
    bs.setBool("status", status)
    bs.setSet("ids", blocks)

    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val r: Row = result.one()
      if (r != null) {
        r.getBool(0)
      } else {
        false
      }
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
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("status", status)
    bs.setInt("block_id", blockId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Assign block to server.
   * @param blockId block id
   * @param serverId server id
   * @return true if success, otherwise false
   */
  override def assignBlockToServer(blockId: Int, serverId: Int): Set[String] = {
    var ps: PreparedStatement = pStatements.getOrElse("AssignBlock", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    var bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("granted_server", serverId)
    bs.setBool("status", true)
    bs.setInt("block_id", blockId)
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
    } else {
      _lastError = sessionManager.lastError
      return null
    }

    // get data
    ps = pStatements.getOrElse("GetBlockData", null)
    if (ps == null || !sessionManager.connected) {
      Logger.error("Cannot connect to database")
      return null
    }
    bs = new BoundStatement(ps)
    bs.setInt("block_id", blockId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (r: Row <- result.all()) {
        return r.getSet("ids", classOf[String]).toSet
      }
      null
    } else {
      _lastError = sessionManager.lastError
      null
    }
  }

  /**
   * Get next free block
   * @return block id >= 0 if success, otherwise -1
   */
  override def getNextBlockId(): Int = {
    val ps: PreparedStatement = pStatements.getOrElse("GetBlockIdsWithStatus", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return -1
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("status", false)
    val result: ResultSet = sessionManager.execute(bs)
    if (result != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (r: Row <- result.all()) {
        if (r != null) {
          return r.getInt("block_id")
        }
      }
      -1
    } else {
      _lastError = sessionManager.lastError
      -1
    }
  }

  /**
   * Free a block with @blockId
   * @param blockId block id
   * @return true if success, otherwise false
   */
  override def freeBlock(blockId: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AssignBlock", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("granted_server", -1)
    bs.setBool("status", false)
    bs.setInt("block_id", blockId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }
}
