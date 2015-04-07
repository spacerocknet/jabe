package spacerock.persistence.cassandra

import java.util

import com.datastax.driver.core._
import models.QuAnModel
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
/**
 * Created by william on 1/13/15.
 */

trait Quiz {
  def getQuizByQid(qid: Long): QuAnModel
  def getQuizzesByCategory(category: String, num: Int): List[QuAnModel]
  def addNewQuiz(qid: Long, category: String, question: String, correctAns: String,
                 ans1: String, ans2: String, ans3: String, df: Int): Boolean
  def updateQuiz(qid: Long, category: String, question: String, correctAns: String,
                 ans1: String, ans2: String, ans3: String, df: Int): Boolean
  def getAllQuizzes(): List[QuAnModel]
  def lastError: Int
}

class QuizDAO (implicit inj: Injector) extends Quiz with Injectable {
  val sessionManager = inject [DbSessionManager]
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()

  var _lastError: Int = Constants.ErrorCode.ERROR_SUCCESS

  def lastError = _lastError

  // initialize prepared statements
  init

  override def updateQuiz(qid: Long, category: String, question: String, rightAnswer: String,
                           ans1: String, ans2: String, ans3: String, df: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateQuiz", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    bs.setString(1, question)
    bs.setString(2, rightAnswer)
    bs.setInt(3, df)
    bs.setString(4, ans1)
    bs.setString(5, ans2)
    bs.setString(6, ans3)
    bs.setLong(7, qid)

    if (sessionManager.execute(bs) != null) {
      if (updateQuizCat(qid, category)) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Add new quiz to quiz bank
   * @param qid quiz id
   * @param category quiz category. In version 1, it only support n-1 relationship between category and quiz
   * @param question question
   * @param rightAnswer right answer
   * @param ans1 answer 1
   * @param ans2 answer 2
   * @param ans3 answer 3
   * @param df
   * @return true if success, otherwise false
   */
  override def addNewQuiz(qid: Long, category: String, question: String, rightAnswer: String,
                          ans1: String, ans2: String, ans3: String, df: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewQuiz", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong(0, qid)
    bs.setString(1, category)
    bs.setString(2, question)
    bs.setString(3, rightAnswer)
    bs.setInt(4, df)
    bs.setString(5, ans1)
    bs.setString(6, ans2)
    bs.setString(7, ans3)

    if (sessionManager.execute(bs) != null) {
      if (updateQuizCat(qid, category)) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  /**
   * Get quiz by quiz id
   * @param qid quiz id
   * @return QuAn model if success, otherwise null
   */
  override def getQuizByQid(qid: Long): QuAnModel = {
    val ps: PreparedStatement = pStatements.get("GetQuizByQid").getOrElse(null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong("qid", qid)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val row: Row = result.one()
      if (row != null) {
        val qa: QuAnModel = new QuAnModel(row.getLong("qid"), row.getString("category"), row.getString("question"),
          row.getString("right_answer"),
          row.getString("ans1"), row.getString("ans2"), row.getString("ans3"),
          row.getInt("df"))
        qa
      } else {
        null
      }
    }
  }

  /**
   * Get all quizzes of requested category
   * @param category category name
   * @param num number of item will be returned
   * @return list of QuAn model if success, otherwise null
   */
  override def getQuizzesByCategory(category: String, num: Int): List[QuAnModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetQuizzesByCategoryI", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: scala.collection.mutable.ListBuffer[QuAnModel] = scala.collection.mutable.ListBuffer()

      val r: Row = result.one()
        if (r != null) {
          val qids: util.Set[java.lang.Long] = r.getSet("qids", classOf[java.lang.Long])
          if (qids != null) {
            // get num of quizzes
            for (qid <- qids) {
              val qa = getQuizByQid(qid)
              if (qa != null)
                l.add(qa)
            }
          }
        }

      l.toList
    }
  }

  /**
   * Get all quizzes from system
   * @return list of QuAn model if success, otherwise false
   */
  override def getAllQuizzes(): List[QuAnModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllQuizzes", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
      null
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: scala.collection.mutable.ListBuffer[QuAnModel] = scala.collection.mutable.ListBuffer()

      for (r: Row <- result.all()) {
        if (r != null) {
          l.add(new QuAnModel(r.getLong("qid"), r.getString("category"), r.getString("question"),
            r.getString("right_answer"),
            r.getString("ans1"), r.getString("ans2"), r.getString("ans3"), r.getInt("df")))
        }
      }
      l.toList
    }
  }

  private def updateQuizCat(qid: Long, category: String): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateQuizCatI", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setSet(0, Set[Long]{qid})
    bs.setString(1, category)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  def init() = {
    _lastError = Constants.ErrorCode.ERROR_SUCCESS
    // update quiz
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.quizzes SET category = ?, " +
      "question = ?, right_answer = ?, df = ?, ans1 = ?, ans2 = ?, ans3 = ? where qid = ?;")
    if (ps != null)
      pStatements.put("UpdateQuiz", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.quizzes_category SET qids = qids + ? " +
      "WHERE category = ?;")
    if (ps != null)
      pStatements.put("UpdateQuizCatI", ps)
    else
      _lastError = sessionManager.lastError

    // Add new quiz
    ps = sessionManager.prepare("INSERT INTO spacerock.quizzes (qid, category, question, right_answer, df, ans1, ans2, ans3) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("AddNewQuiz", ps)
    else
      _lastError = sessionManager.lastError

    // Get quizzes info
    ps = sessionManager.prepare("SELECT * from spacerock.quizzes where qid = ?;")
    if (ps != null)
      pStatements.put("GetQuizByQid", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("SELECT qids from spacerock.quizzes_category where category = ?;")
    if (ps != null)
      pStatements.put("GetQuizzesByCategoryI", ps)
    else
      _lastError = sessionManager.lastError

    // Get all quizzes
    ps = sessionManager.prepare("SELECT * FROM spacerock.quizzes ALLOW FILTERING;")
    if (ps != null)
      pStatements.put("GetAllQuizzes", ps)
    else
      _lastError = sessionManager.lastError
  }
}
