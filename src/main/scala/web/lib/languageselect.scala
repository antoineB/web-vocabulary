package web.lib


import net.liftweb._
import common.{Box, Full, Empty}
import http._
import js.JsCmd
import js.JsCmds.{Noop, SetHtml}
import util.Helpers._
import scala.xml.NodeSeq

import lib.typesafe._

//TODO non support des 2 cas particulier: 0 language, 1 language
abstract class LanguageSelect { 

  def languages: List[String];
  def select2Id: String;

  val languages1 = languages.map(s => s -> s.capitalize)
  var languages2 = languages1.tail


/*  val lang1Res = new Language(languages1.head._1)
  val lang2Res = new Language(languages2.head._1)*/



  var lang1Res = {if (S.param("lang1").isEmpty) languages1.head._1 else S.param("lang1").get}
  var lang2Res = {if (S.param("lang2").isEmpty) languages2.head._1 else S.param("lang2").get}
  val translation = new EnabledTranslation(lang1Res + "-" + lang2Res)

  def ajaxSelectFunc(s: String): JsCmd = { 
    languages2 = languages1.filter(e => e._1 != s)
    lang1Res = s
    translation.value = lang1Res + "-" + lang2Res
    SetHtml(select2Id, select2NodeSeq)
  }


  def selectFunc(s: String) { 
    lang2Res = s
    translation.value = lang1Res + "-" + lang2Res
  }


  def select1NodeSeq = SHtml.ajaxSelect(languages1, Full(lang1Res), ajaxSelectFunc)


  def select2NodeSeq = SHtml.select(languages2, Full(lang2Res), selectFunc)
}






