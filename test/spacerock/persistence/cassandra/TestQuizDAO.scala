package spacerock.persistence.cassandra

import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
// TODO what about change category for quizzes or games
object TestQuizDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val quiz = inject [Quiz]
      assert(quiz.addNewQuiz(1, "cat 1", "Where are you 1?", "Home", "Work 1", "Work 2", "Work 3", 1))
      assert(quiz.addNewQuiz(2, "cat 2", "Where are you 2?", "Home", "Work 1", "Work 2", "Work 3", 3))
      assert(quiz.addNewQuiz(3, "cat 5", "Where are you 3?", "Home", "Work 1", "Work 2", "Work 3", 4))
      assert(quiz.addNewQuiz(4, "cat 7", "Where are you 4?", "Home", "Work 1", "Work 2", "Work 3", 5))
      assert(quiz.addNewQuiz(5, "cat 2", "Where are you 5?", "Home", "Work 1", "Work 2", "Work 3", 6))

      println(quiz.getAllQuizzes())

      println(quiz.getQuizByQid(5))
      println(quiz.getQuizByQid(7))

      println(quiz.getQuizzesByCategory("cat 1", 1))
      println(quiz.getQuizzesByCategory("cat 2", 2))
      println(quiz.getQuizzesByCategory("cat 10", 10))

      assert(quiz.updateQuiz(5, "cat 2", "Where are you 5 ***?", "Home", "Work 1", "Work 2", "Work 3", 19))

      println(quiz.getQuizByQid(5))
    }
  }
}
