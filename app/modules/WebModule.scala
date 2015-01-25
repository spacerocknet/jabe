package modules

import controllers._
import scaldi.Module

class WebModule extends Module {
  binding to new Application
  binding to new SubscriberController
  binding to new GameController
  binding to new NewQuizController
  binding to new UserController
  binding to new CategoryController
}
