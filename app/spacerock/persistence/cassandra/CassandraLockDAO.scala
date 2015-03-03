package spacerock.persistence.cassandra

import com.datastax.driver.core._
import play.Logger
import scaldi.{Injectable, Injector}

import scala.collection.JavaConversions._

/**
 * Created by william on 2/24/15.
 */

trait CassandraLock {
//  def lock(key: String): Boolean
  def tryLock(key: String): Boolean
  def unlock(key: String): Boolean
  def close(): Unit
}

class CassandraLockDAO (implicit inj: Injector) extends CassandraLock with Injectable {
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
    // Lock
    var ps: PreparedStatement = session.prepare("UPDATE spacerock.lock SET do_lock = ? WHERE lock_name = ? if do_lock = ?;")
    pStatements.put("Lock", ps)

    ps = session.prepare("INSERT INTO spacerock.lock (lock_name, do_lock) " +
      "VALUES (?, ?) IF NOT EXISTS;")
    pStatements.put("InsertLock", ps)

    // unlock
    ps = session.prepare("UPDATE spacerock.lock SET do_lock = ? WHERE lock_name = ?;")
    pStatements.put("Unlock", ps)

  }

  override def close() = {
    if (cluster != null)
      cluster.close()
  }

  override def tryLock(key: String): Boolean = {
    val ps: PreparedStatement = pStatements.get("Lock").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool(0, true)
    bs.setString("lock_name", key)
    bs.setBool(2, false)
    var result: ResultSet = session.execute(bs)
    var row: Row = result.one()
    if (row != null) {
      if (!row.getBool(0)) {
        // if lock is not exists, try to insert new record
        val ps2: PreparedStatement = pStatements.get("InsertLock").getOrElse(null)
        if (ps2 == null || !isConnected) {
          Logger.error("Cannot connect to database")
          return false
        }
        val bs2: BoundStatement = new BoundStatement(ps2)
        bs2.setString("lock_name", key)
        bs2.setBool("do_lock", false)
        session.execute(bs2)
        // try to lock again
        result = session.execute(bs)
        row = result.one()
        if (row != null) {
          return row.getBool(0)
        }
      } else {
        return true
      }
    }
    false
  }

  override def unlock(key: String): Boolean = {
    val ps: PreparedStatement = pStatements.get("Unlock").getOrElse(null)
    if (ps == null || !isConnected) {
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setBool("do_lock", false)
    bs.setString("lock_name", key)
    val result: ResultSet = session.execute(bs)
    val row: Row = result.one()
    if (row != null) {
      row.getBool(0)
    } else {
      false
    }
  }
}
