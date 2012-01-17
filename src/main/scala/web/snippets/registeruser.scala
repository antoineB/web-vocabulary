package web.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq

import bl.ConcreteBL

import lib.typesafe._

class RegisterUser {
  var userName = new UserName("")
  var password = new Password("")
  var email = new Email("")

  def process() { 
    val errors = ConcreteBL.registerUser(userName, password, email)
    
    if (errors.isEmpty) 
      S.redirectTo("/")
    else
      errors.keys.foreach(k => S.error(k + "-err", errors(k)))
  }

  def render = {

    "name=user-name" #> SHtml.onSubmit(userName.value = _) &
    "name=password" #> SHtml.onSubmit(password.value = _) &
    "name=email" #> SHtml.onSubmit(email.value = _) &
    "type=submit" #> SHtml.onSubmitUnit(process) 
  }
}

