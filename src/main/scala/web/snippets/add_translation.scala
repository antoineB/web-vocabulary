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
import web.lib.LanguageSelect

class AddTranslation extends LanguageSelect {
  val sourceWord = new Word("")
  val targetWord = new ListWord("")

  override def select2Id = "target-language"
  override def languages = ConcreteBL.allLanguagesList

  //  val translations = ConcreteBL.allEnableTranslations.map(t => t._1 + "-" + t._2)
  //  val translation = new EnabledTranslation(translations.head)

  def process() { 
    val errors = ConcreteBL.addTranslation(sourceWord, targetWord, translation)

    if (errors.isEmpty)
      S.redirectTo(S.uri + "?lang1=" + lang1Res + "&lang2=" + lang2Res)
    else //TODO rediriger aussi en erreur idem au dessus
      errors.keys.foreach(k => S.error(k + "-err", errors(k)))
  }

  def render = {
    
    "#source-language" #> select1NodeSeq &
    "#target-language" #> select2NodeSeq &
    "name=word-from" #> SHtml.onSubmit(sourceWord.value = _) &
    "name=word-to" #> SHtml.onSubmit(targetWord.value = _) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}
