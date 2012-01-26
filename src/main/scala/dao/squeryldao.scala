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
  private val defaultWeaknessLimit = 10

  //----LANGUAGE

  def allLanguages(): List[Language] = transaction { from(DB.languages)(l => select(l)).toList }

  def existLanguage(name: String): Boolean = transaction { from(DB.languages)(l => where(l.name === name) select(l)).isEmpty.unary_! }

  def getLanguage(name: String): Option[Language] = transaction { 
    from(DB.languages)(l => where(l.name === name) select(l)).headOption
  }

  def addLanguage(name: String, iso: String = ""): Boolean = { 
    if ("""[a-z_]+""".r.findAllIn(name).isEmpty)
      false
    else if("""[a-z]{1,4}""".r.findAllIn(iso).isEmpty)
      false
    else if (existLanguage(name))
      false
    else { 
      transaction { DB.addNewLanguage(name, iso) }
      true
    }
  }



  //----WORD
  def allWords(langName: String, n: Option[Int] = None): List[Word] = 
    n match {
      case None => transaction { from(DB.words(langName))(w => select(w)).page(0, defaultWordLimit).toList }
      case Some(res) => transaction { from(DB.words(langName))(w => select(w)).page(0, res).toList }
    }
  

  def allWordsStartWith(langName: String, start: String, n: Option[Int] = None): List[Word] =  
    n match {
      case None => transaction { from(DB.words(langName))(
	w => 
	  where(w.name like (start + "%")) select(w)).page(0, defaultWordLimit).toList }
      case Some(res) => transaction { from(DB.words(langName))(
	w => 
	  where(w.name like (start + "%")) select(w)).page(0, res).toList }
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



  //----USER
  def existUser(name: String): Boolean = transaction { 
    from(DB.users)(u => where(u.name like name) select(u)).isEmpty.unary_!
  }
  
  def addUser(name: String, password: String, email: String): Boolean = { 
    if (existUser(name))
      false
    else  {
      transaction { DB.users.insert(new User(name, password, email)) }
      true
    }
  }

  def verifyUser(name: String, pass: String): Option[User] = transaction { 
    from(DB.users)(u => where((u.name like name) and (u.password like pass)) select(u)).headOption
  }
  

  /** Eeturn the translation table for the given languages.
   */ 
  private def getTrTable(sourceLang: String, targetLang: String) = 
    DB.translations.get(sourceLang) match { 
      case None => DB.translations(targetLang)(sourceLang)
      case Some(res) => res(targetLang)
    }


  //----LEARNING
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
	  val trTable = getTrTable(sourceLanguage, targetLanguage)

	  val lw = new LearningWord(userId, 
				    wordId, 
				    sourceLanguageId, 
				    targetLanguageId, 0, 0, 0.0F)

	transaction { 
	  DB.learningWords.insert(lw)
	  
	  val l = getWordTranslations(wordId, sourceLanguage, targetLanguage)
	  l.foreach(
	    e => {  
	      DB.translationScores.insert(
		new TranslationScore(lw.id, e.id, 0))
	      e.nbRelations += 1
	      trTable.update(e)
	  } )
	}
	true
      }
    }
    else
      false
  }

  private def getWordTranslations(wordId: Long, sourceLanguage: String, targetLanguage: String): List[Translation] =
    DB.translations.get(sourceLanguage) match { 
      case None => inTransaction { from(DB.translations(targetLanguage)(sourceLanguage))(
	tr => where(tr.targetWordId === wordId) select(tr)).toList }
      case Some(res) => inTransaction { from(res(targetLanguage))(
	tr => where(tr.sourceWordId === wordId) select(tr)).toList }
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

  def removeLearningWord(userId: Long, name: String, srcLang: String, trgLang: String): Boolean = transaction { 
   val srcLangId: Long = from(DB.languages)(
      l => where(l.name === srcLang) select(l.id)).head
    val trgLangId: Long = from(DB.languages)(
      l => where(l.name === trgLang) select(l.id)).head
    val wordId = from(DB.words(srcLang))(w => where(w.name === name) select(w.id)).head

    DB.learningWords.deleteWhere(lw => (lw.wordId === wordId) and (lw.sourceLanguageId === srcLangId) and (lw.targetLanguageId === trgLangId))

  true
  }


  private def languageIds: HashMap[Long, String] = transaction {
    from(DB.languages)(l => select(l)).foldLeft(HashMap.newBuilder[Long, String])(
      (m, e) => m += (e.id -> e.name)).result
  }


  def LearningWord(userId: Long): scala.collection.mutable.HashMap[String, ListBuffer[(String, List[String])]] = {
    import scala.collection.mutable.HashMap 
    val langs = languageIds
    val m = HashMap[String, ListBuffer[(String, List[String])]]()
    transaction { 
      val q = from(DB.learningWords)(lw => where(lw.userId === userId) select(lw))
      q.foreach(
	e => { 
	  val trgLang = langs(e.targetLanguageId)
	  val srcLang = langs(e.sourceLanguageId)
	  val wordName = from(DB.words(srcLang))(l => where(l.id === e.wordId) select(l.name)).head
	  
	  val allTrans = DB.translations.get(srcLang) match { 
	    case None => from(DB.translations(trgLang)(srcLang), DB.words(trgLang))(
	      (tr, w) => where((tr.targetWordId === e.wordId) and
			  (w.id === tr.sourceWordId)) select(w.name))

	    case Some(res) => from(res(trgLang), DB.words(trgLang))(
	      (tr, w) => where((tr.sourceWordId === e.wordId) and
			  (w.id === tr.targetWordId)) select(w.name))
	  }
	  val key = srcLang + "-" + trgLang
	  val res = wordName -> allTrans.toList

	  if (!m.contains(key))
	    m.update(key, ListBuffer(res))
	  else 
	    m(key) += (res)
	}
      )
    }
    m
  }
    

 def getLearning(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Option[LearningWord] = { 
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

  def learningFail(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean = 
    getLearning(userId, name, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => { 
        lw.fails += 1
	lw.average = lw.success.toFloat / (lw.success + lw.fails)
	transaction { DB.learningWords.update(lw) }
	true
      }
    }
  
  
  def learningSuccess(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean = 
    getLearning(userId, name, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => {
	lw.success += 1
	lw.average = lw.success.toFloat / (lw.success + lw.fails)
	transaction { DB.learningWords.update(lw) }
	true
      }
    }
  


  private def existTranslation(table: Table[Translation], sourceWordId: Long, targetWordId: Long): Option[Translation] = 
    inTransaction { from(table)(tr => 
      where((tr.sourceWordId === sourceWordId) and (tr.targetWordId === targetWordId)) select(tr)).headOption }

  private def wordIncreaseRelations(wordId: Long, lang: String) { 
    inTransaction { 
      val w = from(DB.words(lang))(w => where(w.id === wordId) select(w)).head
      w.nbRelations += 1
      DB.words(lang).update(w)
    }
  }

  private def translationIncreaseRelations(wordId: Long, sourceLang: String, targetLang: String) { 
    inTransaction { 
      val tr = DB.translations.get(sourceLang) match { 
	case None => from(DB.translations(targetLang)(sourceLang))(
	  tr => where(tr.targetWordId === wordId) select(tr)).head
	case Some(res) => from(res(targetLang))(
	  tr =>  where(tr.sourceWordId === wordId) select(tr)).head
      }
      tr.nbRelations += 1
      getTrTable(sourceLang, targetLang).update(tr)
    }
  }

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

     
      DB.translations.get(sourceLanguage) match { 
	case None => 
	  if (existTranslation(DB.translations(targetLanguage)(sourceLanguage), targetWordId, sourceWordId).isEmpty)
	    transaction { DB.translations(targetLanguage)(sourceLanguage).insert(
	      new Translation(targetWordId, sourceWordId)) }
	case Some(res) => 
	  if (existTranslation(res(targetLanguage), sourceWordId, targetWordId).isEmpty)
	    transaction { res(targetLanguage).insert(
	      new Translation(sourceWordId, targetWordId)) }
      }

    wordIncreaseRelations(sourceWordId, sourceLanguage)
    wordIncreaseRelations(targetWordId, targetLanguage)
    
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
   

    if (ok)
	DB.translations.get(sourceLanguage) match { 
	  case None => transaction { 
	    from(DB.translations(targetLanguage)(sourceLanguage))(
	      tr =>
		where((tr.sourceWordId === targetWordId) and
		      (tr.targetWordId === sourceWordId)) select(tr.id)
	    ).isEmpty.unary_! }
	  case Some(res) => transaction { 
	    from(res(targetLanguage))(
	      tr =>
		where((tr.sourceWordId === sourceWordId) and
		      (tr.targetWordId === targetWordId)) select(tr.id)
	    ).isEmpty.unary_! }
	}
    else
      false
  }
  
  /** Get all possible word that are transation of the given word id.
   */ 
  private def allWordTranslation(wordId: Long, sourceLanguage: String, targetLanguage: String): List[Word] =
    DB.translations.get(sourceLanguage) match { 
      case None => inTransaction { 
	from(
	  DB.translations(targetLanguage)(sourceLanguage),
	  DB.words(targetLanguage))(
	    (tr, w) => where(
	      (tr.targetWordId === wordId) and 
	      (tr.sourceWordId === w.id)) select(w)).toList }
      case Some(res) => inTransaction { 
	from(
	  DB.translations(sourceLanguage)(targetLanguage),
	  DB.words(targetLanguage))(
	    (tr, w) => where(
	      (tr.sourceWordId === wordId) and 
	      (tr.targetWordId === w.id)) select(w)).toList }
    }
  
  /** Get all possible translation for a word.
   */ 
  def allWordTranslation(wordName: String, sourceLanguage: String, targetLanguage: String): List[Translation] = { 
    val w = getWord(wordName, sourceLanguage)
    if (w.isEmpty) 
      List()
    else 
      DB.translations.get(sourceLanguage) match { 
	case None => transaction { 
	  from(DB.translations(targetLanguage)(sourceLanguage))(
	    tr => where(tr.targetWordId === w.get.id) select(tr)).toList }
	  case Some(res) => transaction { 
	    from(DB.translations(sourceLanguage)(targetLanguage))(
	      tr => where(tr.sourceWordId === w.get.id) select(tr)).toList }
      }
  }
  

  /** Generate all possible couple of translation.
   */ 
  private def enabledTranslation = {
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


  private def getWordName(transId: Long, sourceLanguage: String, targetLanguage: String) = 
    DB.translations.get(sourceLanguage) match { 
      case None => inTransaction { 
	val trans = from(DB.translations(targetLanguage)(sourceLanguage))(
	  tr => where(tr.id === transId) select(tr)).head
	from(DB.words(sourceLanguage))(
	  w => where(w.id === trans.targetWordId) select(w.name)).head
      }
      case Some(res) => inTransaction { 
	val trans = from(res(targetLanguage))(
	  tr => where(tr.id === transId) select(tr)).head
	from(DB.words(sourceLanguage))(
	  w => where(w.id === trans.sourceWordId) select(w.name)).head
      }
    }


  private def allTranslationScores(learningWordId: Long) = inTransaction { 
    from(DB.translationScores)(ts => where(ts.learningWordId === learningWordId) select(ts)).toList }

  def updateLearningWord(userId: Long, answer: Option[String], w: String, sourceLanguage: String, targetLanguage: String): Boolean =  
    getLearning(userId, w, sourceLanguage, targetLanguage) match { 
      case None => false
      case Some(lw) => { 
	answer match { 
	  case None => lw.fails += 1
	  case Some(res) => { 
	    lw.success += 1
	    val tr = allTranslationScores(lw.id).find(e => 
	      getWordName(e.translationId, sourceLanguage, targetLanguage) == res)
	    if (!tr.isEmpty) { 
	      val ttr = tr.get
	      ttr.success += 1
	      transaction { DB.translationScores.update(ttr) }
	    }
	  }
	}
	lw.average = lw.success.toFloat / (lw.success + lw.fails)
	transaction { DB.learningWords.update(lw) }
	true
      }
    }
  

  def getLearningWordScore(userId: Long, w: String, sourceLanguage: String, targetLanguage: String): Float =  
    getLearning(userId, w, sourceLanguage, targetLanguage) match { 
      case None => 0.0F
      case Some(lw) => lw.average
    }
  


  def increaseFailTranslation(sourceWord: String, sourceLang: String, targetWord: String, targetLang: String): Boolean = { 
    try { 
      val srcWordId = getWord(sourceWord, sourceLang).get.id
      val trgWord = getWord(targetWord, targetLang).get
      val trgWordId = trgWord.id
      
      transaction { 
	val trans = DB.translations.get(sourceLang) match { 
	  case None => 
	    from(DB.translations(targetLang)(sourceLang))( 
	      tr => where(
		(tr.targetWordId === srcWordId) and 
		(tr.sourceWordId === trgWordId)) select(tr)).head 
	  case Some(res) => 
	    from(res(targetLang))(
	      tr => where(
		(tr.sourceWordId === srcWordId) and 
		(tr.targetWordId === trgWordId)) select(tr)).head 
	}

	if (trans.weakNess >= defaultWeaknessLimit && allWordTranslation(srcWordId, sourceLang, targetLang).size > 1)
	  DB.translations.get(sourceLang) match { 
	    case None => DB.translations(targetLang)(sourceLang).deleteWhere(tr => tr.id === trans.id)
	    case Some(table) => table(targetLang).deleteWhere(tr => tr.id === trans.id)
	    if (trgWord.nbRelations < 2)
	      DB.words(targetLang).deleteWhere(w => trgWordId === w.id)
	  }
	else { 
	  trans.weakNess += 1
	  DB.translations.get(sourceLang) match { 
	    case None => DB.translations(targetLang)(sourceLang).update(trans)
	    case Some(table) => table(targetLang).update(trans) 
	  }
	}
      }
      true
    }
    catch { 
      case _ => false
    }
  }


}


