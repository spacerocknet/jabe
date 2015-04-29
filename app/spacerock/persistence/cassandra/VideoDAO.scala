package spacerock.persistence.cassandra

import com.datastax.driver.core.{ResultSet, BoundStatement, PreparedStatement}
import models.{VideoModel}
import play.Logger
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


/**
 * Created by william on 4/22/15.
 */
trait Video {
  def addNewVideoInfo(videoId: Long, videoName: String, shortDes: String, fullDes: String,
                      viewCount: Long, link: String, publishedTime: Long): Boolean
  def getVideoById(videoId: Long) : VideoModel
  def getVideoByName(videoName: String): List[VideoModel]
  def updateWatch(uid: String, videoId: Long, watchedTime: Long): Boolean
  def isWatched(uid: String, dayInEpoch: Int): Boolean
  def lastError: Int
}

class VideoDAO (implicit inj: Injector) extends Video with Injectable {
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
    var ps: PreparedStatement = sessionManager.prepare("UPDATE spacerock.video SET video_name = ?, " +
      "short_description = ?, full_description = ?," +
      "view_count = ?, link = ?, published_time = ? WHERE video_id = ?;")
    if (ps != null)
      pStatements.put("AddNewVideoInfo", ps)
    else
      _lastError = sessionManager.lastError

    ps = sessionManager.prepare("UPDATE spacerock.video_name_id SET video_id = ? " +
      "WHERE video_name = ?;")
    if (ps != null)
      pStatements.put("AddNewVideoInfoRev", ps)
    else
      _lastError = sessionManager.lastError

    // Get video info by id
    ps = sessionManager.prepare("SELECT * from spacerock.video where video_id = ?;")
    if (ps != null)
      pStatements.put("GetVideoById", ps)
    else
      _lastError = sessionManager.lastError

    // Get video info by name
    ps = sessionManager.prepare("SELECT video_id from spacerock.video_name_id where video_name = ?;")
    if (ps != null)
      pStatements.put("GetIdByName", ps)
    else
      _lastError = sessionManager.lastError

    // Insert new watching video
    ps = sessionManager.prepare("INSERT INTO spacerock.uid_video (uid, day_epoch, video_id, watched_time) VALUES " +
      "(?, ?, ?, ?);")
    if (ps != null)
      pStatements.put("UpdateWatchUidVideo", ps)
    else
      _lastError = sessionManager.lastError

    // Insert new watching video
    ps = sessionManager.prepare("INSERT INTO spacerock.video_uid (uid, video_id, watched_time) VALUES " +
      "(?, ?, ?);")
    if (ps != null)
      pStatements.put("UpdateWatchVideoUId", ps)
    else
      _lastError = sessionManager.lastError

    // check to see if the user has watched a video / day
    ps = sessionManager.prepare("SELECT * from spacerock.video_name_id where video_id = ? AND " +
      "day_epoch = ?;")
    if (ps != null)
      pStatements.put("CheckWatchesPerDay", ps)
    else
      _lastError = sessionManager.lastError
  }

  override def addNewVideoInfo(videoId: Long, videoName: String, shortDes: String, fullDes: String,
                               viewCount: Long, link: String, publishedTime: Long): Boolean = {
    var ps: PreparedStatement = pStatements.getOrElse("AddNewVideoInfo", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    var bs: BoundStatement = new BoundStatement(ps)
    bs.setString("video_name", videoName)
    bs.setString("short_description", shortDes)
    bs.setString("full_description", fullDes)
    bs.setLong("view_count", viewCount)
    bs.setString("link", link)
    bs.setLong("published_time", publishedTime)
    bs.setLong("video_id", videoId)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS

      ps = pStatements.getOrElse("AddNewVideoInfoRev", null)
      if (ps == null || !sessionManager.connected) {
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
        Logger.error("Cannot connect to database")
        return false
      }
      bs = new BoundStatement(ps)
      bs.setString("video_name", videoName)
      bs.setLong("video_id", videoId)
      if (sessionManager.execute(bs) != null) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        _lastError = sessionManager.lastError
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def getVideoById(videoId: Long): VideoModel = {
    val ps: PreparedStatement = pStatements.getOrElse("GetVideoById", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setLong(0, videoId)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      for (row <- result.all()) {
        if (row != null) {
          return new VideoModel(row.getLong("video_id"), row.getString("video_name"),
            row.getString("short_description"), row.getString("full_description"),
            row.getLong("view_count"), row.getString("link"),
            row.getLong("published_time"))
        }
      }
    }
    null
  }

  override def getVideoByName(videoName: String): List[VideoModel] = {
    val ps: PreparedStatement = pStatements.getOrElse("GetIdByName", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return null
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString(0, videoName)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[VideoModel] = new ListBuffer[VideoModel]
      var temp: VideoModel = null
      for (row <- result.all()) {
        if (row != null) {
          temp = getVideoById(row.getLong("video_id"))
          if (temp != null)
            l.add(temp)
        }
      }
      return l.toList
    }
    null
  }

  override def updateWatch(uid: String, videoId: Long, watchedTime: Long): Boolean = {
    var ps: PreparedStatement = pStatements.getOrElse("UpdateWatchUidVideo", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    var bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("day_epoch", (watchedTime / 3600 / 24).toInt)
    bs.setLong("video_id", videoId)
    bs.setLong("watched_time", watchedTime)

    if (sessionManager.execute(bs) != null) {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS

      ps = pStatements.getOrElse("UpdateWatchVideoUId", null)
      if (ps == null || !sessionManager.connected) {
        _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
        Logger.error("Cannot connect to database")
        return false
      }
      bs = new BoundStatement(ps)
      bs.setString("uid", uid)
      bs.setLong("video_id", videoId)
      bs.setLong("watched_time", watchedTime)
      if (sessionManager.execute(bs) != null) {
        _lastError = Constants.ErrorCode.ERROR_SUCCESS
        true
      } else {
        _lastError = sessionManager.lastError
        false
      }
    } else {
      _lastError = sessionManager.lastError
      false
    }
  }

  override def isWatched(uid: String, dayInEpoch: Int): Boolean = {
    val ps: PreparedStatement = pStatements.getOrElse("CheckWatchesPerDay", null)
    if (ps == null || !sessionManager.connected) {
      _lastError = Constants.ErrorCode.CassandraDb.ERROR_CAS_NOT_INITIALIZED
      Logger.error("Cannot connect to database")
      return false
    }
    val bs: BoundStatement = new BoundStatement(ps)
    bs.setString("uid", uid)
    bs.setInt("day_epoch", dayInEpoch)
    val result: ResultSet = sessionManager.execute(bs)
    if (result == null) {
      _lastError = sessionManager.lastError
    } else {
      _lastError = Constants.ErrorCode.ERROR_SUCCESS
      val l: ListBuffer[VideoModel] = new ListBuffer[VideoModel]
      var temp: VideoModel = null
      for (row <- result.all()) {
        if (row != null) {
          return true
        }
      }
    }
    false
  }
}
