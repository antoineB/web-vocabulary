package bootstrap.liftweb

import net.liftweb.widgets.autocomplete._
import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import _root_.java.sql.{Connection, DriverManager}
import _root_.com.mysql.jdbc.Driver
import _root_.net.liftweb.mapper.{ConnectionManager,DB,ConnectionIdentifier,DefaultConnectionIdentifier, Schemifier}

import dao.Mysql
import web.session.UserSession

class Boot {
  def boot {

    AutoComplete.init

    // where to search snippet
    LiftRules.addToPackages("web")

    // Build SiteMap
    def sitemap(): SiteMap = SiteMap(
      Menu.i("index") / "index",
      Menu.i("add translation") / "add_translation",
      Menu.i("add language") / "add_language",
      Menu.i("register") / "register_user" >> If(() => !S.loggedIn_?, "Unregister to register again"),
      Menu.i("add_learning") / "add_learning" >> If(() => S.loggedIn_?, "You must be logged in"),
     Menu.i("quizz") / "quizz" >> If(() => S.loggedIn_?, "you must be logged in")
    )

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemap())

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    Mysql.startDbSession

    LiftRules.loggedInTest = Full(() => !UserSession.isEmpty)
  }
}

//https://github.com/lift/lift/blob/master/framework/lift-base/lift-webkit/src/main/scala/net/liftweb/builtin/snippet/TestCond.scala
//def loggedIn_? : Boolean = LiftRules.loggedInTest.map(_.apply()) openOr false
