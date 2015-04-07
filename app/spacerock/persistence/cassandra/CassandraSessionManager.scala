package spacerock.persistence.cassandra

import com.datastax.driver.core.exceptions._
import com.datastax.driver.core._
import play.Logger
import scaldi.{Injector, Injectable}
import spacerock.constants.Constants

/**
 * Created by william on 3/12/15.
 * This class if for manage connection to cassandra cluster.
 * It contains method to connect, reconnect, execute statements...
 */

trait DbSessionManager {
  def connect(): Boolean
  def reconnect(): Boolean
  def execute (stmt: Statement): ResultSet
  def prepare(sql: String) : PreparedStatement
  def connected(): Boolean
  def lastError: Int
  def close()
}

/**
 * This class will manage connections to cassandra cluster
 * @param inj Injector
 */
 class CassandraSessionManager (implicit inj: Injector) extends DbSessionManager with Injectable {

  val clusterName = inject [String] (identified by "cassandra.cluster")
  val casHost: String = inject [String] (identified by "cassandra.seeds.host")
  val casPort: Int = inject [Int] (identified by "cassandra.seeds.port")
  var cluster: Cluster = null

  val errorExceptionString = "Execute statement error with following exception: %s"
  var _lastError: Int = 0

  // connect to database
  var connected: Boolean = connect()
  // get cassandra session after connection is created
  var _session: Session = getSession

  def lastError = _lastError

  def session = _session

  def reconnect(): Boolean = {
    if (!connected || !_session.isClosed) {
      connected = connect()
    }

    connected
  }
  /**
   * Connect to cassandra
   * @return true if connection is successful, false if all nodes are dead
   */
  def connect(): Boolean = {
    cluster = Cluster.builder.addContactPoint(casHost).withPort(casPort).build
    cluster.init
    val metadata = cluster.getMetadata
    if (metadata.getAllHosts.size < 1) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NO_HOST_AVAILABLE
      false
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      true
    }
  }

  /**
   * Get cassandra session
   * @return session if success or null if error occurs
   */
  private def getSession: Session = {
    try {
      if (connected) {
        _session = cluster.connect()
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        return _session
      }
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
    } catch {
      case nha: NoHostAvailableException=>
        Logger.error(errorExceptionString format nha)
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NO_HOST_AVAILABLE
      case au: AuthenticationException =>
        Logger.error(errorExceptionString format au)
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_AUTHENTICATION
      case ise: IllegalStateException =>
        Logger.error(errorExceptionString format ise)
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_ILLEGAL_STATE
    }
    return null
  }

  /**
   * Close cassandra cluster manager
   */
  override def close(): Unit = {
    if (cluster != null && connected)
      cluster.close()
  }

  /**
   * Execute a statement. This function execute in the session and catch exceptions thrown out to record the errors
   * @param stmt statement
   * @return ResultSet instance
   */
  override def execute(stmt: Statement): ResultSet = {
    if (connected) {
      try {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        return _session.execute(stmt)
      } catch {
        case nae: NoHostAvailableException => {
          Logger.error(errorExceptionString format nae)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NO_HOST_AVAILABLE
        }
        case qee: QueryExecutionException => {
          Logger.error(errorExceptionString format qee)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_QUERY_EXECUTION
        }
        case qve: QueryValidationException => {
          Logger.error(errorExceptionString format qve)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_QUERY_VALIDATION
        }
        case ufe: UnsupportedFeatureException => {
          Logger.error(errorExceptionString format ufe)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_UNSUPPORTED_FEATURE
        }
      }
      return null
    } else {
      Logger.error("Connection to cassandra cluster is not initialized")
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      return null
    }
  }

  override def prepare(sql: String): PreparedStatement = {
    if (connected) {
      try {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        return _session.prepare(sql)
      } catch {
        case nae: NoHostAvailableException => {
          Logger.error(errorExceptionString format nae)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NO_HOST_AVAILABLE
        }
        case qee: QueryExecutionException => {
          Logger.error(errorExceptionString format qee)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_QUERY_EXECUTION
        }
        case qve: QueryValidationException => {
          Logger.error(errorExceptionString format qve)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_QUERY_VALIDATION
        }
        case ufe: UnsupportedFeatureException => {
          Logger.error(errorExceptionString format ufe)
          _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_UNSUPPORTED_FEATURE
        }
      }
      return null
    } else {
      Logger.error("Connection to cassandra cluster is not initialized")
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      return null
    }
  }
}
