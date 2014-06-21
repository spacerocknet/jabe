package modules

import service.{OfficialMessageService, SimpleMessageService, MessageService}
import spacerock.persistence.{UserDataDAO, SubscriberDataDAO}
import scaldi.Module
import scaldi.play.condition._
import spacerock.persistence.UserDataDAO
import spacerock.utils.UuidGenerator
import spacerock.utils.UuidMacBasedGenerator

class UserModule extends Module {
  bind [MessageService] when (inDevMode or inTestMode) to new SimpleMessageService
  bind [MessageService] when inProdMode to new OfficialMessageService
  
  bind [UserDataDAO] to new SubscriberDataDAO
  bind [UuidGenerator] to new UuidMacBasedGenerator
}
