package models

/**
 * Created by william on 4/22/15.
 */
case class AvatarModel(avatarId: Long, hairId: Long, hatId: Long, shirtId: Long, shoesId: Long) {
  final val fmt =
    """
      |{
      |"avatar_id" : %d,
      |"hair_id" : %d,
      |"hat_id" : %d,
      |"shirt_id" : %d,
      |"shoes_id" : %d
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(avatarId, hairId, hatId, shirtId, shoesId)
  }
}
