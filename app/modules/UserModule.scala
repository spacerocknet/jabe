package modules

import scaldi.Module
import spacerock.cache.redis.{RedisWrapperDAO, RedisWrapper}
import spacerock.persistence.cassandra._
import spacerock.utils.{IdGenerator, UidGenerator}

class UserModule extends Module {

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
  bind [GameSession] to new GameSessionDAO
  bind [OpenGameSession] to new OpenGameSessionDAO
  bind [UserGameSession] to new UserGameSessionDAO
  
  // cache
  bind [RedisWrapper] to new RedisWrapperDAO
  bind [IdGenerator] to new UidGenerator
}
