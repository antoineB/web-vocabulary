package dao

import dao.entity._

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Schema, KeyedEntity, Table, Session}
import org.squeryl.annotations.ColumnBase

import scala.collection.immutable.HashMap
import java.sql.{Connection, Statement, ResultSet}

object DB extends Schema {
  val words = scala.collection.mutable.HashMap[String, Table[Word]]()
  val translations = scala.collection.mutable.HashMap[String, HashMap[String, Table[Translation]]]()

  val languages = table[Language]("languages")
  val translationTables = table[TranslationTable]("translation_tables")
  val users = table[User]("users")
  val learningWords = table[LearningWord]("learning_words")

  on(languages)(l => declare(
    l.name is(unique),
    l.id is(autoIncremented, primaryKey)
  ))

  on(translationTables)(tt => declare(
    tt.name is(unique),
    tt.id is(autoIncremented, primaryKey)
  ))

  on(users)(u => declare(
    u.id is(autoIncremented, primaryKey),
    u.name is(unique)
  ))

  on(learningWords)(l => declare(
    l.id is(autoIncremented, primaryKey),
    l.average defaultsTo(0.0F),
    l.success defaultsTo(0),
    l.fails defaultsTo(0)
  ))

  /** Modify the schema only
   */ 
  def addNewLanguage(lang: String, iso: String) { 
    if (!words.contains(lang)) { 
      val langs = words.keys.toList

      words(lang) = table[Word](lang + "_words")
      /*on(words(lang))(w => declare(
	w.name is(unique, indexed),
	w.id is(autoIncremented, primaryKey)))*/

      transaction { createWordTable(lang + "_words") 
      languages.insert(new Language(lang, iso)) }

      val map = HashMap.newBuilder[String, Table[Translation]]
      transaction { 
	for (l <- langs) { 
	  val tmp = table[Translation](lang + "_" + l + "_trans")
	  /*on(tmp)(t => declare(t.id is(autoIncremented, primaryKey), 
			       t.sourceWordId is(indexed)))*/
	  map += ((l, tmp))
	  createTranslationTable(lang + "_" + l + "_trans")
	  translationTables.insert(new TranslationTable(lang + "_" + l + "_trans"))
	}
      }
      translations(lang) = map.result
    }
  }

  private def createWordTable(name: String) { inTransaction { 
    val s = Session.currentSession.connection.createStatement()
    s.executeUpdate(
      "CREATE TABLE " + name + " (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(128) UNIQUE NOT NULL);")
      } }

  private  def createTranslationTable(name: String) { inTransaction { 
    val s = Session.currentSession.connection.createStatement()
    s.executeUpdate(
      "CREATE TABLE " + name + " (id BIGINT PRIMARY KEY AUTO_INCREMENT, sourceWordId BIGINT NOT NULL, targetWordId BIGINT NOT NULL);")
    s.executeUpdate("CREATE INDEX " + name + "_si on " + name + "(sourceWordId);")
    s.executeUpdate("CREATE INDEX " + name + "_ti on " + name + "(targetWordId);")
  
  } }

  /** need to be lunch if a database and tables already exist
   */ 
  def init { transaction { 
    for (n <- allWordTableName) { 
      words(n) = table[Word](n + "_words")
    }
  
    for (n <- allTranslationTableName) { 
      val ns = n.split("_")
      if (translations.contains(ns(0))) { 
	translations(ns(0)) += ((ns(1), table[Translation](n)))
      }
      else { 
	translations(ns(0)) = HashMap((ns(1), table[Translation](n)))
      }
    }
  } 
	    println(words); println(translations) }

  private def allWordTableName = from(languages)(l => select(l.name)).toList
  private def allTranslationTableName = from(translationTables)(tt => select(tt.name)).toList
}





import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.MySQLAdapter
 
object Main {

  def startDbSession():Unit = {
    val dbUsername = "test"
    val dbPassword = "test"
    val dbConnection = "jdbc:mysql://localhost:3306/language_bis?useUnicode=true&characterEncoding=utf8"

    Class.forName("com.mysql.jdbc.Driver")
    SessionFactory.concreteFactory = Some(
      () => 
	Session.create(
          java.sql.DriverManager.getConnection(dbConnection, dbUsername, dbPassword),
          new MySQLAdapter)
    )
  }

  def main(args: Array[String]) {
    startDbSession()
    
//     transaction  {  DB.create }
//    DB.addNewLanguage("finish", "fi")
//    DB.addNewLanguage("french", "fr")
//    DB.addNewLanguage("english", "en")
/*    DB.init

    dd.addUser("toto", "bernard", "jean")
    */

   /* inTransaction { 
      Session.currentSession.setLogger(msg => println(msg)) 
      val a = Schema.users.insert(new User("浩aaaaaa", "pass", "toto@mail.org"))
      /*
      val q = from(Schema.users)(s => select(s))
      q.foreach(
      e =>
	println(e.name)
      )*/
    }*/
  }
}
