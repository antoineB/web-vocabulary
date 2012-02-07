package web.snippet

import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import js.JsExp._
import js.JE.JsRaw
import js.JsCmd
import js.JsCmds.{Noop, SetHtml}
import util.Helpers._
import scala.xml.NodeSeq
import scala.collection.Map

import web.session.UserSession
import bl.ConcreteBL

//obtenir la liste des traduction en apprentissage
//obtenir la liste des mot pour un traduction
//obtenir toutes les mots avec leur traduction

class ListLearning { 
  val map = ConcreteBL.listLearningWords(UserSession.is.get)

  def buttonAjax(s: String, lang: String)(): JsCmd = { 
    val split = lang.split("-")
    ConcreteBL.removeLearningWord(UserSession.is.get, s, split(0), split(1))
    JsRaw("var l = $(\".source-word\"); for (var i = 0; l.size(); i++) { if ($(l[i]).text() === \"%s\") { $(l[i]).parent().remove(); break; } }".format(s)).cmd
  }

 

  def render = { 
    var ns: NodeSeq = NodeSeq.Empty
    map.foreach(
      e =>  
	ns ++= <div> { 
	  e._2.foldLeft(<h3 class="title-language">{e._1}</h3>:NodeSeq)(
	    (res, t) => res ++ <div>{t._2.foldLeft(SHtml.ajaxButton(<b>remove</b>, buttonAjax(t._1, e._1) _) ++ <span class="source-word">{t._1}</span>: NodeSeq)((l, w) => l ++ <span class="target-word">{w}</span>)}</div>
	  )
	} </div>
    )
    
    "*" #> ns
  }
}
