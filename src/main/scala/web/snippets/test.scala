package web.snippet

import net.liftweb.widgets.autocomplete._
import net.liftweb._
import net.liftweb.util.FieldError
import common.{Box, Full, Empty}
import http._
import js.JsCmd
import js.jquery.JqJsCmds.AppendHtml
import js.JsCmds.{Noop, SetHtml}
import util.CssSel
import util.Helpers._
import scala.xml.NodeSeq

class Caca { 
  val l = List(("1", "1"),
	       ("2", "2"),
	       ("3", "3")
	     )

  def process() { }

  def acRefresh(s: String, n: Int) = { 
    println("acRefresh")
    Seq()
  }

  def useless(s: String) { }

  def ajax(s: String): JsCmd = { 
    val ac: NodeSeq = AutoComplete("", acRefresh, useless)
    SetHtml("list", ac)
  }

  def render = { 
    "name=number" #> SHtml.ajaxSelect(l, Full("1"), ajax) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
}
