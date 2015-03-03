package models

import scala.collection.mutable.HashSet

/**
 * An entry in the question/answer catalogue.
 *
 * @param qid - a unique identifier
 * @param category - Movies, Sports, Geographies, Musics, Histories, etc
 * @param question - the question
 * @param correctAns - correct answer
 * @param ans1 - ans1
 * @param ans2 - ans2
 * @param ans3 - ans3
 */
case class QuAnModel(qid: Long, category: String, question: String, correctAns: String,
                     ans1: String, ans2: String, ans3: String, df: Int) {
  final val fmt: String =
    """
      |{
      |"qid" : %d,
      |"category" : "%s",
      |"question" : "%s",
      |"correct-answer" : "%s",
      |"ans1" : "%s",
      |"ans2" : "%s",
      |"ans3" : "%s",
      |"df: : "%d"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(qid, category, question, correctAns, ans1, ans2, ans3, df)
  }
}
