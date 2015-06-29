package modules

import controllers._
import scaldi.Module

class WebModule extends Module {
  binding to new Application
  binding to new AuthCodeController
  binding to new BillingController
  binding to new CategoryController
  binding to new GameController
  binding to new QuizController
  binding to new SkuController
  binding to new UserController
  binding to new GameSessionController
}
