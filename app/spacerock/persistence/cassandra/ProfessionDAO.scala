package spacerock.persistence.cassandra

import com.datastax.driver.core.{BoundStatement, PreparedStatement, ResultSet, Row}
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap

/**
 * Created by william on 4/20/15.
 */
trait Profession {
  def addNewProfession(professionName: String, level: String, probMap: Map[String, Int]): Boolean
  def modifyProfessionProb(professionName: String, level: String, probMap: Map[String, Int]): Boolean
  def getProfessionProb(professionName: String, level: String, cat: String): Map[String, Int]
  def deleteProfessionProb(professionName: String, level: String, catName: String): Boolean
  def lastError: Int
}

class ProfessionDAO (implicit inj: Injector) extends Profession with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
            = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // Add profession level probabilities
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.profession_prob SET probs = probs + ? " +
      "WHERE profession_level = ?;")
    if (ps != null)
      pStatements.put("AddProfessionProb", ps)
    else
      _lastError = sessionManager.lastError

    // Get game all probabilities by profession and level
    ps = sessionManager.prepare("SELECT probs from spacerock.profession_prob where profession_level = ?;")
    if (ps != null)
      pStatements.put("GetAllProbByProbLevel", ps)
    else
      _lastError = sessionManager.lastError

    // Delete prob by cat
    ps = sessionManager.prepare("DELETE probs[?] FROM spacerock.profession_prob WHERE profession_level = ?;")
    if (ps != null)
      pStatements.put("DeleteProbByCat", ps)
    else
      _lastError = sessionManager.lastError

  }

  override def addNewProfession(professionName: String, level: String, probMap: Map[String, Int]): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddProfessionProb", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setMap("probs", probMap)
    bs.setString("profession_level", professionName + "_" + level)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getProfessionProb(professionName: String, level: String, cat: String): Map[String, Int] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllProbByProbLevel", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, professionName + "_" + level)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row: Row <- result.all()) {
        val r = row.getMap("probs", classOf[String], classOf[Int])
        if (cat != null) {
          return HashMap{cat -> r.get(cat)}
        } else {
          return r.toMap
        }
      }
    }
    null
  }

  override def modifyProfessionProb(professionName: String, level: String, probMap: Map[String, Int]): Boolean = {
    addNewProfession(professionName, level, probMap)
  }

  override def deleteProfessionProb(professionName: String, level: String, catName: String): Boolean = ???
}
