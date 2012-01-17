package web.snippet

import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import util.Helpers._
import scala.xml.NodeSeq

import lib.typesafe._
import bl.ConcreteBL

class AddWord {
  val wordName = new Word("")
  val langName = new Language("")


  def process() {
    val errors = ConcreteBL.addWord(wordName, langName)
    
    if (errors.isEmpty) 
      S.redirectTo("/")
    else
      errors.keys.foreach(k => S.error(k + "-err", errors(k)))
  }

  def render = {

    val lang = ConcreteBL.allLanguages
    
    "name=word-name" #> SHtml.onSubmit(wordName.value = _) &
    "name=language" #> SHtml.select(lang, Full(lang.head._2), 
				    (s => langName.value = s)) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}
