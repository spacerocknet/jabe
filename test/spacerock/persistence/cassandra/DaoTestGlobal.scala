package spacerock.persistence.cassandra

import play.api.GlobalSettings
import scaldi.play.ScaldiSupport

/**
 * Created by william on 3/7/15.
 */
object DaoTestGlobal extends GlobalSettings with ScaldiSupport {
  def applicationModule = new DaoTestModule
}
