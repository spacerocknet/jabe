package controllers

import models.OpenGameSessionModel
import play.api.Logger
import play.api.mvc._
import play.api.mvc.Controller
import scaldi.Injectable
import scaldi.Injector
import spacerock.persistence.cassandra.OpenGameSession
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import shared.Shared
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration
import scala.actors.threadpool.TimeUnit
import scala.concurrent.duration.FiniteDuration
import spacerock.usergame.GameSessionScanner
import spacerock.usergame.GameSessionUtil


class Application(implicit inj: Injector) extends Controller with Injectable {
  val openGameSessionDao = inject[OpenGameSession]
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler
  val task = new GameSessionScanner()
  implicit val executor = actorSystem.dispatcher

  scheduler.schedule(
        initialDelay = FiniteDuration(1, "seconds"),
        interval = FiniteDuration(10, "seconds"),
        runnable = task)
  
  def index = Action {
     Ok(views.html.index("Welcome"))
  }
  
  def init = Action {
     Ok(views.html.index("Run initialization"))
  }
  
  def testTakeOne = Action {
     val openGameSession = GameSessionUtil.takeAnOpenGameSession
     
     Ok(views.html.index("I got an open game session: " + openGameSession.gameSessionId))
  }
}

