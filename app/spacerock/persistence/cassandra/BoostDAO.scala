package spacerock.persistence.cassandra

/**
 * Created by william on 4/22/15.
 */

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet}
import models.BoostModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._

/**
 * Created by william on 4/22/15.
 */
trait Boost {
  def updateBoost(boostId: Int, canDo: Int, description: String): Boolean
  def getBoostById(boostId: Int): BoostModel
  def lastError: Int
}

class BoostDAO (implicit inj: Injector) extends Boost with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add new boost
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.boost SET can_do = ?, " +
      "description = ? WHERE boost_id = ?;")
    if (ps != null)
      pStatements.put("AddNewBoost", ps)
    else
      _lastError = sessionManager.lastError

    // Get boost by id
    ps = sessionManager.prepare("SELECT * from spacerock.boost where boost_id = ?;")
    if (ps != null)
      pStatements.put("GetBoostById", ps)
    else
      _lastError = sessionManager.lastError


  }

  override def updateBoost(boostId: Int, canDo: Int, description: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewBoost", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("can_do", canDo)
    bs.setString("description", description)
    bs.setInt("boost_id", boostId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getBoostById(pieceId: Int): BoostModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetPieceById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt(0, pieceId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new BoostModel(row.getInt("boost_id"), row.getInt("can_do"),
            row.getString("description"))
        }
      }
    }
    null
  }
}

