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
case class QuAn(qid: Long, category: String, question: String, correctAns: String, ans1: String, ans2: String, ans3: String, df: Int)

object QuAn {

  var questions:Map[String, HashSet[QuAn]] = Map(
                                           "Geographies" -> HashSet(QuAn(0L, "Geographies", "Where is USA", "North America", "Europe", "South Africa", "Asia", 1)),
                                           "Movies" -> HashSet(QuAn(1L, "Movies", "Who directed Avatar' in 2009", "James Cameron", "Werner Herzog", "Sam Raimi", "Kathryn Bigelow", 1),
                                                           QuAn(2L, "Movies", "Who is the director of the 2010 movie Kick Ass", "Mathew Vaughn", "Spike Lee", "Ridley Scott", "Peter Jackson", 1),
                                                           QuAn(3L, "Movies", "In Men in Black 3, if Will Smith is J, Tommy Lee Jones is K, who is O", "Emma Thompson", "Nicolas Cage", "Josh Brolin", "Lady Gaga", 1)),
                                           "Sports" -> HashSet(QuAn(0L, "Sports", "What team did Steve Nash play for before signing with the Lakers in 2012", "Phoenix Suns", "Miami Heat", "New York Knicks", "San Antonio Spurs", 1))
                                        )


  def getQuestions: Map[String, HashSet[QuAn]]  = {
    return questions
  }

  def hasDataForCategory(cat: String): Boolean = {
      println("normalizeCat(cat) == " + normalizeCat(cat))
      return questions.contains(normalizeCat(cat)) 
  }

  def normalizeCat(cat: String): String = {
      var lowerCaseCat : String = cat.toLowerCase()
      if ("geographies" == lowerCaseCat || "geography" == lowerCaseCat)
         return "Geographies"
      else if ("movies" == lowerCaseCat || "movie" == lowerCaseCat)
         return "Movies"
      else if ("sports" == lowerCaseCat || "sport" == lowerCaseCat)
         return "Sports"
      else if ("musics" == lowerCaseCat || "music" == lowerCaseCat)
         return "Musics"
      else return cat
  }

  /**
   * The questions with the given qid.
   */
  def findById(cat: String, qid: Long): Option[QuAn] = {
       if (questions.contains(cat)) {
         val subSet = questions(normalizeCat(cat))
         return subSet.find(_.qid == qid)
       }
       return None
  }

  /**
   * Save a questions to the catalog.
   */
  def save(question: QuAn) = {
    val cat = normalizeCat(question.category)
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
