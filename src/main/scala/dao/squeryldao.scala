package dao

import dao.entity._

import org.squeryl.{Table, Session, Query}
import org.squeryl.PrimitiveTypeMode._

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

class SquerylDAO extends DAO { 
  DB.init
  private val defaultWordLimit = 30
  private val defaultUserLearingWordLimit = 50

  //Language
  def allLanguages(): List[Language] = transaction { from(DB.languages)(l => select(l)).toList }

  def existLanguage(name: String): Boolean = transaction { from(DB.languages)(l => where(l.name === name) select(l)).isEmpty.unary_! }

  def getLanguage(name: String): Option[Language] = transaction { 
    from(DB.languages)(l => where(l.name === name) select(l)).headOption
  }

  def addLanguage(name: String, iso: String = ""): Boolean = transaction { 
    if ("""[a-z_]+""".r.findAllIn(name).isEmpty)
      false
    else if("""[a-z]{1,4}""".r.findAllIn(iso).isEmpty)
      false
    else if (existLanguage(name))
      false
    else { 
      DB.addNewLanguage(name, iso) 
      true
    }
  }

  type Error[T] = Either[HashMap[String, String], T]

  //word
  def allWords(langName: String, n: Option[Int] = None): List[Word] = transaction { 
    n match {
      case None => from(DB.words(langName))(w => select(w)).page(0, defaultWordLimit).toList
      case Some(res) => from(DB.words(langName))(w => select(w)).page(0, res).toList
    }
  }

  def allWordsStartWith(langName: String, start: String, n: Option[Int] = None): List[Word] = transaction { 
    n match {
      case None => from(DB.words(langName))(
	w => 
	  where(w.name like (start + "%")) select(w)).page(0, defaultWordLimit).toList
      case Some(res) => from(DB.words(langName))(
	w => 
	  where(w.name like (start + "%")) select(w)).page(0, res).toList
    }
  }

  def existWord(name: String, langName: String): Boolean = transaction { 
    from(DB.words(langName))(w => where(w.name like name) select(w)).isEmpty.unary_!
  }

  def getWord(name: String, langName: String): Option[Word] = transaction { 
    from(DB.words(langName))(w => where(w.name like name) select(w)).headOption
  }

  def addWord(name: String, langName: String): Boolean = { 
    val w = new Word(name)
    transaction { DB.words(langName).insert(w) }
    true
  }



  //user
  def existUser(name: String): Boolean = transaction { 
    from(DB.users)(u => where(u.name like name) select(u)).isEmpty.unary_!
  }
  
  def addUser(name: String, password: String, email: String): Boolean = { 
    if (existUser(name))
      false
    else transaction {
      DB.users.insert(new User(name, password, email))
      true
    }
  }

  def verifyUser(name: String, pass: String): Option[User] = 
    transaction { 
      val q: Query[User] = from(DB.users)(
	u =>
	  where((u.name like name) and (u.password like pass)) select(u))
      
      if (q.isEmpty)
	None
      else
	Some(q.head)
    }
  


  //learning
  def addLearningWord(wordName: String, userId: Long, sourceLanguage: String, targetLanguage: String): Boolean = { 
    if (getLearning(userId, wordName, sourceLanguage, targetLanguage).isEmpty) { 
      var ok = true

      val targetLanguageId: Long = getLanguage(targetLanguage) match { 
	  case None => ok = false; 0
	case Some(res) => res.id
      }
      val sourceLanguageId: Long = getLanguage(sourceLanguage) match {
	case None => ok = false; 0
	case Some(res) => res.id
      }
      val wordId: Long = getWord(wordName, sourceLanguage) match {
	case None => ok = false; 0
	case Some(res) => res.id 
      }

      if (!ok)
	false
      else { 
	transaction { 
	  DB.learningWords.insert(
	    new LearningWord(userId, 
			     wordId, 
			     sourceLanguageId, 
			     targetLanguageId, 0, 0, 0.0F)) 
	}
	true
      }
    }
    else
      false
  }


  def allUserLearningWord(userId: Long) = transaction { 
    from(DB.learningWords)(lw => where(lw.userId === userId) select(lw))
  }

  def learningWord(userId: Long, limit: Int, sourceLanguage: String, targetLanguage: String): Iterable[(Word, List[Word])] = { 
    var ok = true
   
    val targetLanguageId: Long = getLanguage(targetLanguage) match { 
      case None => ok = false; 0
      case Some(res) => res.id
    }
    val sourceLanguageId: Long = getLanguage(sourceLanguage) match {
      case None => ok = false; 0
      case Some(res) => res.id
    }
    
    if (!ok)
      Iterable.empty
    transaction { 
      val q = from(DB.learningWords, DB.words(sourceLanguage))(
	(lw, sw) => 
	  where(
	    (lw.userId === userId) and 
	    (lw.targetLanguageId === targetLanguageId) and 
	    (lw.sourceLanguageId === sourceLanguageId) and
	    (sw.id === lw.wordId)
	  ) select(sw) orderBy(lw.average asc)).page(0, limit)

      q.map(w => ((w, allWordTranslation(w.id, sourceLanguage, targetLanguage))))
    }
  }

  private def getLearning(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Option[LearningWord] = { 
    var ok = true
   
    val targetLanguageId: Long = getLanguage(targetLanguage) match { 
      case None => ok = false; 0
      case Some(res) => res.id
    }
    val sourceLanguageId: Long = getLanguage(sourceLanguage) match {
      case None => ok = false; 0
      case Some(res) => res.id
    }

    if (!ok)
      None
    else transaction { 
    from(DB.learningWords, DB.words(sourceLanguage))(
	(lw, sw) => 
	  where(
	    (lw.userId === userId) and 
	    (lw.targetLanguageId === targetLanguageId) and 
	    (lw.sourceLanguageId === sourceLanguageId) and
	    (lw.wordId === sw.id) and
	    (sw.name like name)
	  ) select(lw)).headOption
    }
  }

  def learningFail(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean = transaction { 
    getLearning(userId, name, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => { 
        lw.fails += 1
	lw.average = lw.success / (lw.success + lw.fails)
	DB.learningWords.insert(lw)
	true
      }
    }
  }
  
  def learningSuccess(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean = transaction {
    getLearning(userId, name, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => {
	lw.success += 1
	lw.average = lw.success / (lw.success + lw.fails)
	DB.learningWords.insert(lw)
	true
      }
    }
  }


  private def existTranslation(table: Table[Translation], sourceWordId: Long, targetWordId: Long): Option[Translation] = from(table)(
    tr => 
      where((tr.sourceWordId === sourceWordId) and (tr.targetWordId === targetWordId)) select(tr)).headOption
    

  //translation
  def addTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean = { 
    val sourceWordId = getWord(sourceWordName, sourceLanguage) match {
      case None => { 
	addWord(sourceWordName, sourceLanguage)
	getWord(sourceWordName, sourceLanguage).get.id
      }
      case Some(res) => res.id
      
    }
    var targetWordId = getWord(targetWordName, targetLanguage) match { 
      case None => { 
	addWord(targetWordName, targetLanguage)
	getWord(targetWordName, targetLanguage).get.id
      }
      case Some(res) => res.id
    }

    transaction { 
      DB.translations.get(sourceLanguage) match { 
	case None => 
	  if (existTranslation(DB.translations(targetLanguage)(sourceLanguage), targetWordId, sourceWordId).isEmpty)
	    DB.translations(targetLanguage)(sourceLanguage).insert(
	      new Translation(targetWordId, sourceWordId))
	case Some(res) => 
	  if (existTranslation(res(targetLanguage), sourceWordId, targetWordId).isEmpty)
	    res(targetLanguage).insert(
	      new Translation(sourceWordId, targetWordId))
      }
    }
    true
  }

  def existTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean = { 
    var ok = true

    val sourceWordId = getWord(sourceWordName, sourceLanguage) match {
      case None => ok = false; 0
      case Some(res) => res.id
    }
    
    val targetWordId = getWord(targetWordName, targetLanguage) match {
      case None => ok = false; 0
      case Some(res) => res.id
    }
   

    if (ok) { 
      transaction { 
	DB.translations.get(sourceLanguage) match { 
	  case None => from(DB.translations(targetLanguage)(sourceLanguage))(
	    tr =>
	      where((tr.sourceWordId === targetWordId) and
		    (tr.targetWordId === sourceWordId)) select(tr.id)
	  ).isEmpty.unary_!
	  case Some(res) => from(res(targetLanguage))(
	  tr =>
	    where((tr.sourceWordId === sourceWordId) and
		  (tr.targetWordId === targetWordId)) select(tr.id)
	  ).isEmpty.unary_!
	}
      }
    }
    else
      false
  }

  private def allWordTranslation(wordId: Long, sourceLanguage: String, targetLanguage: String): List[Word] =
    DB.translations.get(sourceLanguage) match { 
      case None => 
	from(
	  DB.translations(targetLanguage)(sourceLanguage),
	  DB.words(targetLanguage))(
	    (tr, w) => where(
	      (tr.targetWordId === wordId) and 
	      (tr.sourceWordId === w.id)) select(w)).toList
      case Some(res) => 
	from(
	  DB.translations(sourceLanguage)(targetLanguage),
	  DB.words(targetLanguage))(
	    (tr, w) => where(
	      (tr.sourceWordId === wordId) and 
	      (tr.targetWordId === w.id)) select(w)).toList
    }
  

  def allWordTranslation(wordName: String, sourceLanguage: String, targetLanguage: String): List[Translation] = { 
    val w = getWord(wordName, sourceLanguage)
    if (w.isEmpty) List()
    else {
      transaction { 
	DB.translations.get(sourceLanguage) match { 
	  case None => 
	    from(DB.translations(targetLanguage)(sourceLanguage))(
	      tr => where(tr.targetWordId === w.get.id) select(tr)).toList
	  case Some(res) => 
	    from(DB.translations(sourceLanguage)(targetLanguage))(
	      tr => where(tr.sourceWordId === w.get.id) select(tr)).toList
	}
      }
    }
  }

  private lazy val enabledTranslation = { 
    val l = ListBuffer[(String, String)]()
    DB.translations.keys.foreach(
      k => 
	DB.translations(k).keys.foreach(
	  kk =>
	    l += ((k, kk)) += ((kk, k))
	))
    l.toList
  }

  def allEnableTranslation = enabledTranslation
  
  def existTranslation(tr: (String, String)): Boolean = 
    allEnableTranslation.contains(tr)

  def allEnableTranslation(userId: Long): List[(String, String)] = transaction { 
    from(DB.learningWords, DB.languages, DB.languages)(
      (lw, l, l2) =>
	where((lw.userId === userId) and
	    (lw.sourceLanguageId === l.id) and
	    (lw.targetLanguageId === l2.id)) select(l.name, l2.name)
    ).distinct.toList
  }

  def numberLearningWords(userId: Long, trans: (String, String)): Long = transaction { 
    val srcLangId: Long = from(DB.languages)(
      l => where(l.name === trans._1) select(l.id)).head
    val trgLangId: Long = from(DB.languages)(
      l => where(l.name === trans._2) select(l.id)).head

    from(DB.learningWords)(
      lw =>
	where((lw.userId === userId) and
	      (lw.sourceLanguageId === srcLangId) and
	      (lw.targetLanguageId === trgLangId)) compute(count(lw.id))).head.measures
  }

  def updateLearningWord(userId: Long, b: Boolean, w: String, sourceLanguage: String, targetLanguage: String): Boolean = transaction { 
        getLearning(userId, w, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => { 
	if (b)
	  lw.success += 1
	else
          lw.fails += 1
	lw.average = lw.success / (lw.success + lw.fails)
	DB.learningWords.insert(lw)
	true
      }
	}
  }

}
