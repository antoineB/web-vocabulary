package web.snippet

import net.liftweb.widgets.autocomplete._
import net.liftweb.builtin.snippet.Msg
import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import js.JsCmd
import js.jquery.JqJsCmds.AppendHtml
import js.JsCmds.{Noop, SetHtml, Replace}
import util.Helpers._
import scala.xml.NodeSeq
import scala.collection.mutable.Stack

import lib.typesafe._
import bl.ConcreteBL
import web.session.UserSession

class AddLearning {
  var wordCount = 1
  val words: Stack[Word] = Stack()

  val translations = ConcreteBL.allEnableTranslations.map(t => t._1 + "-" + t._2)
  val translation = new EnabledTranslation(translations.head)

  //may fail if database is empty
  var autoCompleteWordLang = new Language(translation.value.get._1)

  def process() { 
    ConcreteBL.addLearningWord(UserSession.is.get, words, translation) match { 
      case None => S.redirectTo("/")
      case Some(res) => S.error("global-err", res)
    }
  }

  def radioCall(s: String) = {
    translation.value = s
    autoCompleteWordLang.value = translation.value.get._1
    Noop
  }

  def refreshWord(text: String, limit: Int) = { 
    ConcreteBL.allWordsStartWith(autoCompleteWordLang, new Word(text))
  }

  def add(): JsCmd = { 
    wordCount += 1
    AppendHtml("add-list", <div id={"learn-" + wordCount}></div>
	     ) & SetHtml("learn-" + wordCount, AutoComplete("", refreshWord, stackIn))
  }

  def del(): JsCmd = { 
    if (wordCount > 1) { 
      wordCount -= 1
      Replace("learn-" + (wordCount + 1), NodeSeq.Empty)
    }
    else
      Noop
  }

  def stackIn(s: String) { 
    words.push(new Word(s))
  }

  def render = {

    "name=word" #> AutoComplete("", refreshWord,  stackIn(_)) &
    "name=language" #> SHtml.ajaxRadio(translations, Full(translations.head), radioCall).toForm &
    "name=add" #> SHtml.ajaxButton("add", add _) &
    "name=del" #> SHtml.ajaxButton("del", del _) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}

