package models

/**
 * Created by william on 4/22/15.
 */
case class BoostModel (boostId: Int, canDo: Int, description: String) {
  final val fmt: String =
    """
      |{
      |"boost_id" : %d,
      |"can_do" : "%s",
      |"description" : "%s"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(boostId, canDo, description)
  }

}
