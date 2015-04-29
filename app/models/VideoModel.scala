package models

/**
 * Created by william on 4/22/15.
 */
case class VideoModel (videoId: Long, videoName: String, shortDescription: String,
                       fullDescription: String, videoCount: Long,
                       link: String, publishedTime: Long) {
  final val fmt =
    """
      |{
      |"video_id" : %d,
      |"video_name" : "%s",
      |"short_description" : "%s",
      | "full_description" : "%s",
      |"video_count": %d,
      |"link" : "%s",
      |"published_time" : "%s"
      |}
    """.stripMargin

  override def toString(): String = {
    fmt.format(videoId, videoName, shortDescription, fullDescription, videoCount, link, publishedTime)
  }
}
