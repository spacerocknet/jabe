package spacerock.persistence.cassandra

import com.datastax.driver.core.{ResultSet, BoundStatement, PreparedStatement}
import models.{AbilityModel}
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import scala.collection.JavaConversions._

/**
 * Created by william on 4/22/15.
 */
trait Ability {
  def addNewAbility(abilityId: Int, abilityName: String, description: String, value: Int): Boolean
  def getAbilityInfo(abilityId: Int): AbilityModel

  def lastError: Int
}
class AbilityDAO (implicit inj: Injector) extends Ability with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
  = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init


  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add new ability
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.boost SET ability_name = ?, " +
      "description = ?, value = ? WHERE ability_id = ?;")
    if (ps != null)
      pStatements.put("AddNewAbility", ps)
    else
      _lastError = sessionManager.lastError

    // Get ability by id
    ps = sessionManager.prepare("SELECT * from spacerock.boost where ability_id = ?;")
    if (ps != null)
      pStatements.put("GetAbilityById", ps)
    else
      _lastError = sessionManager.lastError
  }

  override def addNewAbility(abilityId: Int, abilityName: String, description: String, value: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewAbility", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("ability_name", abilityName)
    bs.setInt("value", value)
    bs.setString("description", description)
    bs.setInt("ability_id", abilityId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getAbilityInfo(abilityId: Int): AbilityModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetPieceById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setInt("ability_id", abilityId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new AbilityModel(row.getInt("ability_id"), row.getString("ability_name"),
            row.getString("description"), row.getInt("value"))
        }
      }
    }
    null
  }
}
