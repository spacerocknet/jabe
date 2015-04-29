package spacerock.persistence.cassandra

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet}
import models.PieceModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._

/**
 * Created by william on 4/22/15.
 */
trait Piece {
  def updatePiece(pieceId: Int, categoryName: String, description: String, imageId: String, tittle: String): Boolean
  def getPieceByPieceId(pieceId: Int): PieceModel
  def lastError: Int
}

class PieceDAO (implicit inj: Injector) extends Piece with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add new piece
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.piece SET category_name = ?, " +
      "description = ?, image_id = ?, tittle = ? WHERE piece_id = ?;")
    if (ps != null)
      pStatements.put("AddNewPiece", ps)
    else
      _lastError = sessionManager.lastError

    // Get game result by uid
    ps = sessionManager.prepare("SELECT * from spacerock.piece where piece_id = ?;")
    if (ps != null)
      pStatements.put("GetPieceById", ps)
    else
      _lastError = sessionManager.lastError


  }

  override def updatePiece(pieceId: Int, categoryName: String, description: String,
                           imageId: String, tittle: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewPiece", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("category_name", categoryName)
    bs.setString("description", description)
    bs.setString("image_id", imageId)
    bs.setString("tittle", tittle)
    bs.setInt("piece_id", pieceId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getPieceByPieceId(pieceId: Int): PieceModel = {
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
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new PieceModel(row.getInt("piece_id"), row.getString("category_name"),
                         row.getString("description"), row.getString("image_id"), row.getString("tittle"))
        }
      }
    }
    null
  }
}
