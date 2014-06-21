package modules

import controllers.Application
import scaldi.Module
import controllers.SubscriberController

class WebModule extends Module {
  binding to new Application
  
  binding to new SubscriberController
}
