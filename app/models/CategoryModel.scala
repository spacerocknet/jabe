package models

import play.api.libs.json.Json

/**
 * Created by william on 1/13/15.
 */
case class CategoryModel (category: String, description: String, gameIds: List[Int]) {
  final val fmt =
    """
      |"{
      |category" : "%s",
      |"description" : "%s",
      |"game-ids" : %s
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(category, description, Json.toJson(gameIds))
  }
}
