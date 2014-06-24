package modules

import scaldi.Module
import scaldi.play.condition._

import spacerock.utils.UuidGenerator
import spacerock.utils.UuidMacBasedGenerator
import spacerock.persistence._

class UserModule extends Module {
  
  bind [UserDataDAO] to new SubscriberDataDAO
  bind [UuidGenerator] to new UuidMacBasedGenerator
  
  bind [TAppsConfig] to new AppsConfigDAO
}
