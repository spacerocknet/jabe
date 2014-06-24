package modules

import controllers.Application
import scaldi.Module
import controllers.SubscriberController
import controllers.QuizController

class WebModule extends Module {
  binding to new Application
  binding to new QuizController
  binding to new SubscriberController
  
}
