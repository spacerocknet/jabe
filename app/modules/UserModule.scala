package modules

import scaldi.Module
import spacerock.cache.redis.{RedisWrapperDAO, RedisWrapper}
import spacerock.persistence.cassandra._
import spacerock.utils.{IdGenerator, UidGenerator}

class UserModule extends Module {

  // persistence
  bind [DbSessionManager] to new CassandraSessionManager

  bind [AuthCode] to new AuthCodeDAO
  bind [Billing] to new BillingDAO
  bind [CassandraLock] to new CassandraLockDAO
  bind [Category] to new CategoryDAO
  bind [Device] to new DeviceDAO
  bind [GameInfo] to new GameInfoDAO
  bind [GameResult] to new GameResultDAO
  bind [Quiz] to new QuizDAO
  bind [Sku] to new SkuDAO
  bind [UidBlock] to new UidBlockDAO
  bind [UserData] to new UserDataDAO

  // cache
  bind [RedisWrapper] to new RedisWrapperDAO
  bind [IdGenerator] to new UidGenerator
}
