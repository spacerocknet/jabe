package models

/**
 * Created by william on 4/22/15.
 */
case class AbilityModel (abilityId: Int, abilityName: String, description: String, value: Int) {
  final val fmt =
    """
      |{
      |"ability_id" : %d,
      |"ability_name" : "%s",
      |"description" : "%s",
      |"value" : %d
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(abilityId, abilityName, description, value)
  }
}
