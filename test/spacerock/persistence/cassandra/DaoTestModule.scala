package spacerock.persistence.cassandra

import play.GlobalSettings
import scaldi.Module
import spacerock.cache.redis.{RedisWrapper, RedisWrapperDAO}
import spacerock.utils.{IdGenerator, UidGenerator}

/**
 * Created by william on 3/7/15.
 */
class DaoTestModule extends GlobalSettings with Module {
  binding identifiedBy "cassandra.cluster" to "Test Cluster"
  binding identifiedBy "cassandra.seeds.host" to "127.0.0.1"
  binding identifiedBy "cassandra.seeds.port" to 9042
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
  bind [ServerInfo] to new ServerInfoDAO

  // cache
  bind [RedisWrapper] to new RedisWrapperDAO
  bind [IdGenerator] to new UidGenerator

}
