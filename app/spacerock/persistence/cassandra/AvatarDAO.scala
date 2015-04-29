package spacerock.persistence.cassandra

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet}
import models.AvatarModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._

/**
 * Created by william on 4/22/15.
 */
trait Avatar {
  def createNewAvatar(uid: String, avatarId: Long, hairId: Long, hatId: Long, shirtId: Long, shoesId: Long): Boolean
  def changeAvatar(uid: String, avatarId: Long): Boolean
  def modifyHair(avatarId: Long, hairId: Long): Boolean
  def modifyHat(avatarId: Long, hatId: Long): Boolean
  def modifyShirt(avatarId: Long, shirtId: Long): Boolean
  def modifyShoes(avatarId: Long, shoesId: Long): Boolean
  def getAvatar(avatarId: Long): AvatarModel

  def lastError: Int
}

class AvatarDAO (implicit inj: Injector) extends Avatar with Injectable {

  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add new video info
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.avatar SET hair_id = ?, " +
      "hat_id = ?, shirt_id = ?, shoes_id = ? WHERE avatar_id = ?;")
    if (ps != null)
      pStatements.put("AddNewAvatar", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.avatar SET hair_id = ? WHERE avatar_id = ?;")
    if (ps != null)
      pStatements.put("UpdateHair", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.avatar SET hat_id = ? WHERE avatar_id = ?;")
    if (ps != null)
      pStatements.put("UpdateHat", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.avatar SET shirt_id = ? WHERE avatar_id = ?;")
    if (ps != null)
      pStatements.put("UpdateShirt", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.avatar SET shoes_id = ? WHERE avatar_id = ?;")
    if (ps != null)
      pStatements.put("UpdateShoes", ps)
    else
      _lastError = sessionManager.lastError


    ps = sessionManager.prepare("UPDATE spacerock.avatar_history SET avatar_id = ? " +
      "WHERE uid = ?;")
    if (ps != null)
      pStatements.put("ChangeAvatar", ps)
    else
      _lastError = sessionManager.lastError

    // Get video info by id
    ps = sessionManager.prepare("SELECT * from spacerock.avatar where avatar_id = ?;")
    if (ps != null)
      pStatements.put("GetAvatarById", ps)
    else
      _lastError = sessionManager.lastError
  }

  override def createNewAvatar(uid: String, avatarId: Long, hairId: Long,
                               hatId: Long, shirtId: Long, shoesId: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewAvatar", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong("avatar_id", avatarId)
    bs.setLong("hair_id", hairId)
    bs.setLong("hat_id", hatId)
    bs.setLong("shirt_id", shirtId)
    bs.setLong("shoes_id", shoesId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS

      changeAvatar(uid, avatarId)
    } else {
      _lastError = sessionManager.lastError
      false
    }

  }

  override def getAvatar(avatarId: Long): AvatarModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAvatarById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong("avatar_id", avatarId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new AvatarModel(row.getLong("avatar_id"), row.getLong("hair_id"),
              row.getLong("hat_id"), row.getLong("shirt_id"), row.getLong("shoes_id"))
        }
      }
    }
    null
  }

  override def modifyShoes(avatarId: Long, shoesId: Long): Boolean = {
    updateItem(avatarId, "UpdateShoes", "shoes_id", shoesId)
  }

  override def modifyShirt(avatarId: Long, shirtId: Long): Boolean = {
    updateItem(avatarId, "UpdateShirt", "shirt_id", shirtId)
  }

  override def changeAvatar(uid: String, avatarId: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("ChangeAvatar", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setLong("avatar_id", avatarId)
    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def modifyHair(avatarId: Long, hairId: Long): Boolean = {
    updateItem(avatarId, "UpdateHair", "hair_id", hairId)
  }

  override def modifyHat(avatarId: Long, hatId: Long): Boolean = {
    updateItem(avatarId, "UpdateHat", "hat_id", hatId)
  }

  private def updateItem(avatarId: Long, key: String, fieldName: String, value: Long): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse(key, null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong("avatar_id", avatarId)
    bs.setLong(fieldName, value)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }
}
