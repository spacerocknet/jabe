package spacerock.persistence

import com.datastax.driver.core._
import models.{QuAn}
import scaldi.{Injectable, Injector}
import scala.collection.JavaConversions._
/**
 * Created by william on 1/13/15.
 */

trait NewQuiz {
  def getQuizByQid(qid: Long): QuAn
  def getQuizzesByCategory(category: String): List[QuAn]
  def addNewQuiz(qid: Long, category: String, question: String, correctAns: String,
                 ans1: String, ans2: String, ans3: String, df: Int): Boolean
  def updateQuiz(qid: Long, category: String, question: String, correctAns: String,
                 ans1: String, ans2: String, ans3: String, df: Int): Boolean
  def getAllQuizzes(): List[QuAn]
}

class NewQuizDAO (implicit inj: Injector) extends NewQuiz with Injectable {
  val clusterName = inject [String] (identified by "cassandra.cluster")
  var cluster: Cluster = null
  var session: Session = null
  val pStatements: scala.collection.mutable.Map[String, PreparedStatement]
                = scala.collection.mutable.Map[String, PreparedStatement]()

  val isConnected: Boolean = connect("127.0.0.1")

  override def updateQuiz(qid: Long, category: String, question: String, rightAnswer: String,
                           ans1: String, ans2: String, ans3: String, df: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("UpdateQuiz", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    bs.setString(1, question)
    bs.setString(2, rightAnswer)
    bs.setInt(3, df)
    bs.setString(4, ans1)
    bs.setString(5, ans2)
    bs.setString(6, ans3)
    bs.setLong(7, qid)

    session.execute(bs)

    true
  }

  override def addNewQuiz(qid: Long, category: String, question: String, rightAnswer: String,
                          ans1: String, ans2: String, ans3: String, df: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("AddNewQuiz", null)
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong(0, qid)
    bs.setString(1, category)
    bs.setString(2, question)
    bs.setString(3, rightAnswer)
    bs.setInt(4, df)
    bs.setString(5, ans1)
    bs.setString(6, ans2)
    bs.setString(7, ans3)

    session.execute(bs)
    true
  }

  override def getQuizByQid(qid: Long): QuAn = {
    val ps: PreparedStatement = pStatements.get("GetQuizByQid").getOrElse(null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong("qid", qid)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      val qa: QuAn = new QuAn(row.getLong("qid"), row.getString("category"), row.getString("question"),
                              row.getString("right_answer"),
                              row.getString("ans1"), row.getString("ans2"), row.getString("ans3"),
                              row.getInt("df"))
      qa
    } else {
      null
    }

  }

  override def getQuizzesByCategory(category: String): List[QuAn] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetQuizzesByCategory", null)

    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, category)
    bs.setInt(1, 2)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[QuAn] = scala.collection.mutable.ListBuffer()

    for (r: Row <- result.all()) {
      l.add(new QuAn(r.getLong("qid"), r.getString("category"), r.getString("question"),
        r.getString("right_answer"),
        r.getString("ans1"), r.getString("ans2"), r.getString("ans3"), r.getInt("df")))
    }
    l.toList
  }

  override def getAllQuizzes(): List[QuAn] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetAllQuizzes", null)

    val bs: BoundStatement = new BoundStatement(ps)
    val result: ResultSet = session.execute(bs)
    val l: scala.collection.mutable.ListBuffer[QuAn] = scala.collection.mutable.ListBuffer()

    for (r: Row <- result.all()) {

      l.add(new QuAn(r.getLong("qid"), r.getString("category"), r.getString("question"),
        r.getString("right_answer"),
        r.getString("ans1"), r.getString("ans2"), r.getString("ans3"), r.getInt("df")))
    }
    l.toList
  }

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
    // update quiz
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.quizzes SET category = ?, " +
      "question = ?, right_answer = ?, df = ?, ans1 = ?, ans2 = ?, ans3 = ? where qid = ?;")
    pStatements.put("UpdateQuiz", ps)

    // Add new quiz
    ps = session.prepare("INSERT INTO spacerock.quizzes (qid, category, question, right_answer, df, ans1, ans2, ans3) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);")
    pStatements.put("AddNewQuiz", ps)

    // Get quizzes info
    ps = session.prepare("SELECT * from spacerock.quizzes where qid = ?;")
    pStatements.put("GetQuizByQid", ps)
    ps = session.prepare("SELECT * from spacerock.quizzes where category = ? LIMIT ? ALLOW FILTERING;")
    pStatements.put("GetQuizzesByCategory", ps)

    // Get all quizzes
    ps = session.prepare("SELECT * FROM spacerock.quizzes ALLOW FILTERING;")
    pStatements.put("GetAllQuizzes", ps)
  }

  def close() = {
    if (cluster != null)
      cluster.close()
  }
}
