package web.snippet

import net.liftweb._
import common.{Box, Empty, Full} 
import http._
import util.Helpers._
import scala.xml.NodeSeq

import lib.typesafe._
import web.session.UserSession
import bl.ConcreteBL

class Login {
  val userName = new UserName("")
  val password = new Password("")

  def process() {
    ConcreteBL.login(userName, password) match { 
      case Left(map) => map.keys.foreach(k => S.error(k + "-err", map(k)))
      case Right(long) => UserSession(Some(long)); S.redirectTo("/")
     }
  }

  def render = {
    "name=user-name" #> SHtml.onSubmit(userName.value = _) &
    "name=password" #> SHtml.onSubmit(password.value = _) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }

  
}
