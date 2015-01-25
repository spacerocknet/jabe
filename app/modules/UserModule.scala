package modules

import scaldi.Module
import scaldi.play.condition._

import spacerock.utils.UuidGenerator
import spacerock.utils.UuidMacBasedGenerator
import spacerock.persistence._

class UserModule extends Module {
  bind [UuidGenerator] to new UuidMacBasedGenerator

  bind [NewUserData] to new NewUserDataDAO

  bind [NewCategory] to new NewCategoryDAO

  bind [NewQuiz] to new NewQuizDAO

  bind [GameConfig] to new GameConfigDAO

  bind [GameResult] to new GameResultDAO

}
