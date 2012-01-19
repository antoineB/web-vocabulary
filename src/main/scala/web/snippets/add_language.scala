package web.snippet

import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import util.Helpers._
import scala.xml.NodeSeq

import lib.typesafe._
import bl.ConcreteBL


class AddLanguage {
  val languageName = new NewLanguage("")
  val isoName = new NewIsoLanguage("")


  def process() {
    val errors = ConcreteBL.addLanguage(languageName, isoName)
    
    if (errors.isEmpty) 
      S.redirectTo("/")
    else
      errors.keys.foreach(k => S.error(k + "-err", errors(k)))
  }

  def render = {

    "name=language-name" #> SHtml.onSubmit(languageName.value = _) &
    "name=iso-name" #> SHtml.onSubmit(isoName.value = _) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}
