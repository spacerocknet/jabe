package controllers

import play.api.mvc._
import scaldi.{Injectable, Injector}
import spacerock.persistence.cassandra.{Category, UserData, Quiz}

import play.api.mvc.Controller

class Application(implicit inj: Injector) extends Controller with Injectable {
  val userDao = inject [UserData]
  val category = inject[Category]
  val quiz = inject[Quiz]

  def index = Action {
     Ok(views.html.index("Welcome"))
  }
}