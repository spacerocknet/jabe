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
case class QuAn(qid: Long, category: String, question: String, correctAns: String, ans1: String, ans2: String, ans3: String)

object QuAn {

  var questions:Map[String, HashSet[QuAn]] = Map(
                                           "Geographies" -> HashSet(QuAn(0L, "Geographies", "Where is USA?", "North America", "Europe", "South Africa", "Asia")),
                                           "Movies" -> HashSet(QuAn(1L, "Movies", "Who directed Avatar' in 2009", "James Cameron", "Werner Herzog", "Sam Raimi", "Kathryn Bigelow"),
                                                           QuAn(2L, "Movies", "Who is the director of the 2010 movie Kick Ass", "Mathew Vaughn", "Spike Lee", "Ridley Scott", "Peter Jackson"),
                                                           QuAn(3L, "Movies", "In Men in Black 3, if Will Smith is J, Tommy Lee Jones is K, who is O", "Emma Thompson", "Nicolas Cage", "Josh Brolin", "Lady Gaga"))
                                        )


  def getQuestions: Map[String, HashSet[QuAn]]  = {
    return questions
  }


  /**
   * The questions with the given qid.
   */
  def findById(cat: String, qid: Long): Option[QuAn] = {
       if (questions.contains(cat)) {
         val subSet = questions(cat)
         return subSet.find(_.qid == qid)
       }
       return None
  }

  /**
   * Save a questions to the catalog.
   */
  def save(question: QuAn) = {
    val cat = question.category
    val qid = question.qid

    findById(cat, qid).map( oldQuestion => {
          questions(cat) -= oldQuestion 
          questions(cat) += question
       }
    ).getOrElse(
       {
        if (!questions.contains(cat)) {
           questions += (cat -> HashSet())
        }
        questions(cat) += question
        //var subSet:HashSet[QuAn] = questions(cat);      
        //subSet += question
        //throw new IllegalArgumentException("Question not found")
       }
    )
  }

   
}
