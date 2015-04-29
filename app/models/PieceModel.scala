package models

/**
 * Created by william on 4/22/15.
 */
/**
 *
 * @param pieceId
 * @param categoryName
 * @param description
 * @param imageId
 * @param tittle
 */
case class PieceModel(pieceId: Int, categoryName: String, description: String, imageId: String, tittle: String) {
  final val fmt: String =
    """
      |{
      |"piece_id" : %d,
      |"category" : "%s",
      |"description" : "%s",
      |"image_id" : "%s",
      |"tittle" : "%s"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(pieceId, categoryName, description, imageId, tittle)
  }
}
