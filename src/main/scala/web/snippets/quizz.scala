package web.snippet

import net.liftweb._
import net.liftweb.util.FieldError
import net.liftweb.builtin.snippet.Msg
import common.{Box, Full, Empty}
import http._
import js.JsCmd
import js.jquery.JqJsCmds.AppendHtml
import js.JsCmds.{Noop, SetHtml}
import util.CssSel
import util.Helpers._
import scala.xml.NodeSeq
import scala.collection.immutable.{HashMap, SortedMap}


import lib.typesafe._
import bl.ConcreteBL
import web.session.UserSession

object Quizz { 
  def createElem(label: String, nb: Int, f: (String) => Unit) = { 
    val ns: NodeSeq = SHtml.text("", f, "id" -> {"elem-" + nb})
    val ns2: NodeSeq = Msg.renderIdMsgs("elem-" + nb + "-err")
    <div>{<span id={"label-" + nb}>{label}</span> ++
	  ns ++ ns2
	}</div>
  }

  def length(nb: Long) = { 
    val l = List.newBuilder[(String, String)]
    if (nb > 5) { 
      l += ("5" -> "5")
      if (nb > 10) { 
	l += ("10" -> "10")
	if (nb > 15) { 
	  l += ("15" -> "15")
	  if (nb > 20) { 
	    l += ("20" -> "20")
	  }
	}
      }
    }
    l.result
  }

}

class Quizz { 
  var currentElems: Int = 0
  var currentTrans: String = null

  var mapB = SortedMap.newBuilder[String, Word]

  def addNewValue(name: String)(value: String) { 
    mapB += (name -> new Word(value))
  }

  def languageSelect(s: String): JsCmd = { 
    currentTrans = s

    var i = -1

    val list = ConcreteBL.getLearning(UserSession.is.get, currentElems, new EnabledTranslation(currentTrans))
    if (currentElems > list.size)
      currentElems = list.size

    list.foldLeft(Noop)(
      (js, v) => { 
	i += 1
	js & SetHtml("label-" + i, <span>{v}</span>)
      })
  }

  def numberSelect(s: String): JsCmd = { 
    currentElems = s.toInt
    mapB = SortedMap.newBuilder[String, Word]

    val list = ConcreteBL.getLearning(UserSession.is.get, currentElems, new EnabledTranslation(currentTrans))

    if (currentElems > list.size)
      currentElems = list.size

    {for (i <- 0 until currentElems) 
     yield AppendHtml("list", 
		      Quizz.createElem(list(i), i, addNewValue(i.toString)))
   }.foldLeft[JsCmd](SetHtml("list", NodeSeq.Empty))(
       (js, jsU) => (js & jsU))
  }

  def process(): JsCmd = {
    val m = ConcreteBL.testQuizz(UserSession.is.get, mapB.result, new EnabledTranslation(currentTrans))
    m.foreach(e => S.error("elem-" + e._1 + "-err", 
			   e._2.foldLeft(NodeSeq.Empty)((ns, n) => ns ++ <span>{n}</span>)))
  }

  def render = { 
    val numbers = List(("5", "5"), ("10", "10"), ("15", "15"), ("20", "20"))
    val translations = ConcreteBL.allEnableTranslations(UserSession.is.get)

    println("render")

    val tr = translations.map(e => (e._1 + "-" + e._2) -> (e._1 + "-" + e._2))

    println(tr)

    currentTrans = tr.head._1
    currentElems = numbers.head._1.toInt


    val list = ConcreteBL.getLearning(UserSession.is.get, currentElems, new EnabledTranslation(currentTrans))

    println("zozo")

    if (currentElems > list.size)
      currentElems = list.size

    "#list *" #> 
      {for (i <- 0 until currentElems) 
       yield Quizz.createElem(list(i), i, addNewValue(i.toString))
     }.foldLeft(NodeSeq.Empty)((nss, ns) => nss ++ ns) &
    "name=language" #> SHtml.ajaxSelect(tr, Full(tr.head._2), languageSelect) &
    "name=number" #> (SHtml.ajaxSelect(numbers, Full(numbers.head._2), numberSelect) ++
    SHtml.hidden(process))
  }
}
