package controllers

import java.util.Date

import models._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import scaldi.{Injectable, Injector}
import spacerock.constants.Constants
import spacerock.persistence.cassandra.{Device, CassandraLock, UidBlock, UserData}
import spacerock.utils.{StaticVariables, IdGenerator}
import scala.collection.JavaConversions._

class UserController (implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [UserData]
  val idGenerator = inject [IdGenerator]
  val uidBlock = inject [UidBlock]
  val idLocker = inject [CassandraLock]
  val device = inject [Device]
  implicit val subscriberFmt = Json.format[SubscriberModel]

  /**
   * Get next block ids from cassandra.
   * @return true if success, otherwise false
   */
  private def getNewBlockIds: Boolean = {
    var count: Int = 0
    var isCreateNew: Boolean = false
    var res: Boolean = false
//    var blocks: Set[Long] = null
    var nextBlock: Set[String] = null
    var canLock: Boolean = idLocker.tryLock(Constants.LOCK_UID_KEY)
    while (!canLock && count < Constants.MAX_LOCK_TRIES) {
      canLock = idLocker.tryLock(Constants.LOCK_UID_KEY)
      count = count + 1
      Thread.sleep(5)
    }
    if (canLock) {
      val nextBlkId: Int = uidBlock.getNextBlockId()
      // runs out of blocks
      if (nextBlkId < 0) {
        // generate new block
        Logger.info("Generate new uid block")
        nextBlock = idGenerator.generateNextBlock(Constants.MAX_UID_BLOCK_SIZE)
//        nextBlock = blocks.map(i => i.toString)
        uidBlock.addNewBlock(idGenerator.generateNextId(Constants.REDIS_BLOCK_ID_KEY).toInt,
                             nextBlock, true, StaticVariables.serverId)
        isCreateNew = true
      }
      if (!isCreateNew) {
        nextBlock = uidBlock.assignBlockToServer(nextBlkId, StaticVariables.serverId)
      }
      nextBlock.foreach(id => StaticVariables.freeIds.add(id))
      // unlock key
      idLocker.unlock(Constants.LOCK_UID_KEY)
      res = true
    }

    res
  }

  /**
   * Generate uuid for client
   * @return client id object if success, otherwise Service unavailable
   */
  def generateUniqueId() = Action {
    var count: Int = 0
    var canLock: Boolean = idLocker.tryLock(Constants.REDIS_CLIENT_KEY)
    while (!canLock && count < Constants.MAX_LOCK_TRIES) {
      canLock = idLocker.tryLock(Constants.REDIS_CLIENT_KEY)
      count = count + 1
    }
    if (canLock) {
      val json = Json.obj("client_id" -> idGenerator.generateNextId(Constants.REDIS_CLIENT_KEY))
      idLocker.unlock(Constants.REDIS_CLIENT_KEY)
      Ok(json)
    } else {
      Ok(StaticVariables.NoIdStatus)
    }
  }

  /**
   * Get all users of system
   * @return
   */
	def getAllUser = Action {
    val result: List[SubscriberModel] = userDao.getAllUsers()
    var seq = Seq[JsObject]()
    if (result != null) {
      for (subscriber <- result) {
        val userString = Json.obj(
          "uid" -> (if (subscriber.uid == null) "" else subscriber.uid),
          "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
          "os" -> (if (subscriber.os == null) "" else subscriber.os),
          "model" -> (if (subscriber.model == null) "" else subscriber.model),
          "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
          "email" -> (if (subscriber.email == null) "" else subscriber.email),
          "fb_id" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
          "state" -> (if (subscriber.state == null) "" else subscriber.state),
          "region" -> (if (subscriber.region == null) "" else subscriber.region),
          "country" -> (if (subscriber.country == null) "" else subscriber.country),
          "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))

        seq = seq :+ userString
      }
      Ok(JsArray(seq))
    } else {
      Ok(Json.obj("status" -> userDao.lastError))
    }
	}

  /**
   * Get user by uid.
   * @param uid user id
   * @return subscriber info if success, empty json object or bad request otherwise.
   */
  def getUserInfoByUID (uid: String) = Action {
    try {
      val subscriber: SubscriberModel = userDao.getInfoByUID(uid)
      if (subscriber != null) {
        val userString = Json.obj(
          "uid" -> (if (subscriber.uid == null) "" else subscriber.uid),
          "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
          "os" -> (if (subscriber.os == null) "" else subscriber.os),
          "model" -> (if (subscriber.model == null) "" else subscriber.model),
          "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
          "email" -> (if (subscriber.email == null) "" else subscriber.email),
          "fb_id" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
          "state" -> (if (subscriber.state == null) "" else subscriber.state),
          "region" -> (if (subscriber.region == null) "" else subscriber.region),
          "country" -> (if (subscriber.country == null) "" else subscriber.country),
          "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))
        Ok(userString)
      } else {
        if (userDao.lastError == Constants.ErrorCode.ERROR_SUCCESS)
          Ok(Json.obj())
        else
          Ok(Json.obj("status" -> userDao.lastError))
      }
    } catch {
      case e: Exception => {
        println("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Get user info by username
   * @return user info if success, otherwise empty json or failed status
   */
  def getUserInfoByUsername = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val userName = (json.getOrElse(null) \ "user_name").asOpt[String].getOrElse(null)
      if (userName != null) {
        val subscribers: List[SubscriberModel] = userDao.getInfoByUsername(userName)
        if (subscribers != null) {
//          val userString = Json.obj(
//            "uid" -> (if (subscriber.uid == null) "" else subscriber.uid),
//            "user_name" -> userName,
//            "platform" -> (if (subscriber.platform == null) "" else subscriber.platform),
//            "os" -> (if (subscriber.os == null) "" else subscriber.os),
//            "model" -> (if (subscriber.model == null) "" else subscriber.model),
//            "phone" -> (if (subscriber.phone == null) "" else subscriber.phone),
//            "email" -> (if (subscriber.email == null) "" else subscriber.email),
//            "fb_id" -> (if (subscriber.fbId == null) "" else subscriber.fbId),
//            "state" -> (if (subscriber.state == null) "" else subscriber.state),
//            "region" -> (if (subscriber.region == null) "" else subscriber.region),
//            "country" -> (if (subscriber.country == null) "" else subscriber.country),
//            "apps" -> (if (subscriber.apps == null) "" else subscriber.apps))

//          Ok(userString)
          Ok(Json.toJson(subscribers))
        } else {
          if (userDao.lastError == Constants.ErrorCode.ERROR_SUCCESS)
            Ok(Json.obj())
          else
            Ok(Json.obj("status" -> userDao.lastError))
        }
      } else {
        Ok(StaticVariables.InputErrorStatus)
      }
    } catch {
      case e: Exception => {
        println("exception = %s" format e)
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Update existed user with some extra information
   * @return
   */
  def updateUserInfo = Action { request =>
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].getOrElse(null)
      val userName = (json.getOrElse(null) \ "user_name").asOpt[String].getOrElse(null)
      val firstName = (json.getOrElse(null) \ "first_name").asOpt[String].getOrElse("")
      val lastName = (json.getOrElse(null) \ "last_name").asOpt[String].getOrElse("")
      val email = (json.getOrElse(null) \ "email").asOpt[String].getOrElse("")
      val fbId = (json.getOrElse(null) \ "fb_id").asOpt[String].getOrElse("")
      val locState = (json.getOrElse(null) \ "state").asOpt[String].getOrElse("")
      val locRegion = (json.getOrElse(null) \ "region").asOpt[String].getOrElse("")
      val locCountry = (json.getOrElse(null) \ "country").asOpt[String].getOrElse("")
      val appName = (json.getOrElse(null) \ "apps").asOpt[String].getOrElse("")
      if (uid != null) {
        if (userDao.addBasicInfo(uid, userName, firstName, lastName, email, fbId,
                            locState, locRegion, locCountry, appName)) {
          val userString = Json.obj(
            "uid" -> uid)
          Ok(userString)
        } else {
          Ok(Json.obj("status" -> userDao.lastError))
        }
      } else {
        Ok(StaticVariables.InputErrorStatus)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Ok(StaticVariables.BackendErrorStatus)
      }
    }
  }

  /**
   * Add user by device's info / register. This method do the following tasks:
   *  - Take an uid for new user from free ids. If the store is empty, it will generate and assign itself to
   *    the created block of ids.
   *  - Insert new subscribe to system
   *  - Insert device's info to system
   *  - Return uid for subscriber
   * @return uid if success, otherwise Service unavailable
   */
  def addUserWithoutInfo = Action { request =>
    var retObj: JsObject = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
      val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
      val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
      val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
      val deviceUuid = (json.getOrElse(null) \ "device_uuid").asOpt[String].getOrElse("")
      var uid: String = null

      // get or generate new uid
      if (StaticVariables.freeIds.isEmpty) {
        if (getNewBlockIds) {
          Logger.info("Get new block ids successful")
        } else {
          Logger.warn("Cannot get new uid block")
        }
      }
      try {
        uid = StaticVariables.freeIds.remove(0)
      } catch {
        case e: Exception => { Logger.info("exception = %s" format e); uid = ""}
      }
      if (uid != null && !uid.equals("")) {
        val subscriber = new SubscriberModel(uid, platform, os, model, phone, deviceUuid)
        var status: Boolean = userDao.addDeviceInfo(subscriber)
        if (status) {
          // add device info
          status = device.addNewDevice(deviceUuid, new Date(System.currentTimeMillis()), uid,
            platform, model, phone)
          if (status) {
            retObj = Json.obj("uid" -> uid)
          } else {
            Logger.warn("User registration error. Please check users and devices tables. %s" format json.toString)
            retObj = Json.obj("status" -> device.lastError)
          }
        } else {
          retObj = Json.obj("status" -> userDao.lastError)
        }
      } else {
        Logger.info("Cannot get uid. %s" format json.toString)
        retObj = StaticVariables.InputErrorStatus
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        retObj = StaticVariables.BackendErrorStatus
      }
    }
    Ok(retObj)
  }

  /**
   * When user changes his device, this information will be updated.
   * There are 2 tables need to update: users and device.
   * @return ok status if success, failed status, bad request if not
    */
  def userChangeDevice = Action { request =>
    var result: JsObject = StaticVariables.OkStatus
    try {
      val json: Option[JsValue] = request.body.asJson

      val uid = (json.getOrElse(null) \ "uid").asOpt[String].orNull
      val os = (json.getOrElse(null) \ "os").asOpt[String].getOrElse("")
      val platform = (json.getOrElse(null) \ "platform").asOpt[String].getOrElse("")
      val phone = (json.getOrElse(null) \ "phone").asOpt[String].getOrElse("")
      val model = (json.getOrElse(null) \ "model").asOpt[String].getOrElse("")
      val deviceUuid = (json.getOrElse(null) \ "device_uuid").asOpt[String].getOrElse("")

      if (uid == null) {
        Ok(StaticVariables.InputErrorStatus)
      } else {
        val subscriber = new SubscriberModel(uid, platform, os, model, phone, deviceUuid)
        var status: Boolean = userDao.addDeviceInfo(subscriber)
        if (status) {
          status = device.addNewDevice(deviceUuid, new Date(System.currentTimeMillis()), uid,
            platform, model, phone)
          if (status) {
            result = StaticVariables.OkStatus
          } else {
            result = Json.obj("status" -> device.lastError)
          }
        } else {
          result = Json.obj("status" -> userDao.lastError)
        }
      }
    } catch {
      case e:Exception => {
        Logger.info("exception = %s" format e)
        result = StaticVariables.BackendErrorStatus
      }
    }
    Ok(result)
  }
}