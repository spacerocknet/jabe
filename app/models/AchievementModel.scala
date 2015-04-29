package models

/**
 * Created by william on 4/22/15.
 */
case class AchievementModel (achievementId: Int, description: String) {
  final val FMT: String =
    """
      |{
      |"achievement_id" : "%d",
      |"description" : "%s"
      |}
    """.stripMargin

  override def toString(): String = {
    FMT.format(achievementId, description)
  }
}
