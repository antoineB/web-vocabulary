package web.snippet

import net.liftweb.widgets.autocomplete._
import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import js.JsCmds.Noop
import util.Helpers._
import scala.xml.NodeSeq

import lib.typesafe._
import bl.ConcreteBL

class AddTranslation {
  val sourceWord = new Word("")
  val targetWord = new Word("")

  val translations = ConcreteBL.allEnableTranslations.map(t => t._1 + "-" + t._2)

  println(translations)

  val translation = new EnabledTranslation(translations.head)

  def process() { 
    val errors = ConcreteBL.addTranslation(sourceWord, targetWord, translation)

    if (errors.isEmpty)
      S.redirectTo("/")
    else
      errors.keys.foreach(k => S.error(k + "-err", errors(k)))
  }

  def render = {
    "name=word-from" #> SHtml.onSubmit(sourceWord.value = _) &
    "name=word-to" #> SHtml.onSubmit(targetWord.value = _) &
    "name=language" #> SHtml.radio(translations, Full(translations.head), translation.value = _).toForm &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}
