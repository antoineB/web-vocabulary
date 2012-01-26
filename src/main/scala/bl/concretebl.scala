package bl

import dao.{DAO, SquerylDAO}
import lib.typesafe._

import scala.collection.immutable.{HashMap, SortedMap}
import scala.collection.mutable.Stack
import java.security.MessageDigest


object ConcreteBL extends BL with SessionVarStorage {
  private lazy val dao: DAO = new SquerylDAO


  def allTranslations = dao.allEnableTranslation


  def registerUser(name: UserName, password: Password, email: Email): HashMap[String, String] = {
    val m = HashMap.newBuilder[String, String]

    val checkedName = name.get match { 
      case None => m += (("user-name", UserName.errMsg)) ; null
      case Some(s) => s
    }
    val checkedPass = password.get match { 
      case None => m += (("password", Password.errMsg)) ; null
      case Some(s) => s
    }
    val checkedEmail = email.get match { 
      case None => m += (("email", Email.errMsg)) ; null
      case Some(s) => s
    }
    
    if (m.result.isEmpty) { 
      if (dao.existUser(checkedName))
	m += (("user-name", "user name already exist"))
      else 
	dao.addUser(checkedName, checkedPass, checkedEmail)
    }
    m.result
  }


  def allLanguages: List[(String, String)] =  
    dao.allLanguages.map(l => (l.name, l.name.capitalize))
  
  def allLanguagesList: List[String] = dao.allLanguages.map(e => e.name)

  def existLanguage(s: String): Boolean = dao.existLanguage(s)


  def addWord(word: Word, lang: Language): HashMap[String, String] = { 
    val m = HashMap.newBuilder[String, String]

    val checkedWord = word.get match { 
      case None => m += (("word-name", Word.errMsg)) ; null
      case Some(s) => s
    }
    val checkedLang = lang.get match { 
      case None => m += (("language-name", Language.errMsg)) ; null
      case Some(s) => s
    }

    if (m.result.isEmpty)
      if (!dao.existWord(checkedWord, checkedLang))
	dao.addWord(checkedWord, checkedLang)
    
    m.result
  }


  def addLanguage(lang: NewLanguage, iso: NewIsoLanguage): HashMap[String, String] = { 
    val m = HashMap.newBuilder[String, String]

    val checkedLanguage = lang.get match {
      case None => m += ("language-name" -> NewLanguage.errMsg) ; null
      case Some(res) => res
    }
    val checkedIso = iso.get match {
      case None => m += ("iso-name" -> NewIsoLanguage.errMsg) ; null
      case Some(res) => res
    }
    
    if (m.result.isEmpty)
      dao.addLanguage(checkedLanguage, checkedIso)

    m.result
  }

  def allEnableTranslations: List[(String, String)] = dao.allEnableTranslation


  def existTranslation(tr: (String, String)): Boolean = dao.existTranslation(tr)


  def allWordsStartWith(lang: Language, w: Word): List[String] = { 
    var errors = false

    val checkedWord = w.get match { 
      case None => errors = true ; null
      case Some(s) => s
    }
    val checkedLang = lang.get match { 
      case None => errors = true ; null
      case Some(s) => s
    }
    
    if (errors)
      List()
    else
      dao.allWordsStartWith(checkedLang, checkedWord, Some(30)).map(e => e.name)
  }


  def addTranslation(sourceWord: Word, targetWord: ListWord, translation: EnabledTranslation): HashMap[String, String] = { 
    val m = HashMap.newBuilder[String, String]

    val checkedSource = sourceWord.get match { 
      case None => m += (("word-from", Word.errMsg)) ; null
      case Some(s) => s
    }
    val checkedTarget = targetWord.get match { 
      case None => m += (("word-to", ListWord.errMsg)) ; null
      case Some(s) => s
    }
    val checkedTranslation = translation.get match { 
      case None => m += (("language", EnabledTranslation.errMsg)); null
      case Some(s) => s
    }
    
    if (m.result.isEmpty)
      checkedTarget.foreach(t => 
	dao.addTranslation(checkedSource, 
			   checkedTranslation._1, t, checkedTranslation._2))
    
    m.result
  }


  def addLearningWord(userId: Long, words: Stack[Word], translation: EnabledTranslation): Option[String] = { 
    val l = List.newBuilder[String]
    var err = false
    
    words.foreach(
      w =>
	w.get match { 
	  case None => err = true
	  case Some(s) => l += s
	}
    )

    if (err)
      Some("there is an error in your request")
    else { 
      l.result.foreach(w => 
	dao.addLearningWord(w, userId, translation.get.get._1, translation.get.get._2))
      None
    }
  }


  def login(userName: UserName, password: Password): Either[HashMap[String, String], Long] = { 
    val m = HashMap.newBuilder[String, String]

    val checkedName = userName.get match { 
      case None => m += (("user-name", UserName.errMsg)) ; null
      case Some(s) => s
    }
    val checkedPass = password.get match { 
      case None => m += (("password", Password.errMsg)) ; null
      case Some(s) => s
    }

    if (m.result.isEmpty) 
      dao.verifyUser(checkedName, checkedPass) match { 
	case None => m += (("global", "the user dont exisit")); Left(m.result)
	case Some(user) => Right(user.id)
      }
    else
      Left(m.result)
  }


  def allEnableTranslations(userId: Long): List[(String, String)] = dao.allEnableTranslation(userId)


  def getLearning(userId: Long, n: Int, trans: EnabledTranslation): List[String] = { 
    if (n > 30) println("bl.quizz n is > 30")
    
    val checkedTranslation = trans.get match { 
      case None => null
      case Some(s) => s
    }

    //si la translation change

    val lw = dao.learningWord(userId, 30, checkedTranslation._1, checkedTranslation._2).toList

    val mapB = HashMap.newBuilder[String, List[String]]
    for ((a, b) <- lw)
      mapB += (a.name -> b.map(_.name))
    
    storeAllLearningWords(mapB.result)


    val rndNb = allLearningWords.size - 1
    val nb = {if (n > allLearningWords.size) allLearningWords.size else n}

    val rnd = new scala.util.Random
    storeSelectedLearningWords({
      for (i <- 0 until nb) 
	yield lw(rnd.nextInt(rndNb))._1.name}.toList)

    selectedLearningWords
  }

  def numberLearningWords(userId: Long, trans: EnabledTranslation): Long = { 
    val checkedTranslation = trans.get match { 
      case None => null
      case Some(s) => s
    }
    
    dao.numberLearningWords(userId, checkedTranslation)
  }

  
  private def isGoodWord(nb: Int, word: String): (Boolean, List[String]) = { 
    val l = allLearningWords(selectedLearningWords(nb))
    val w = word.trim.toLowerCase

    if (l.contains(w))
      (true -> l)
    else
      (false -> l)
  }


  /**
   * @param map is a map containing a number as key and a word entered
   */
  def testQuizz(userId: Long, map: SortedMap[String, Word], trans: EnabledTranslation): HashMap[String, ((Boolean, List[String]), Float)] = { 
    val mapB = HashMap.newBuilder[String, ((Boolean, List[String]), Float)]

    for ((k, v) <- map) { 
      v.get match {
	case None => mapB += (k -> (false -> List("the word is a word conform") -> 0.0F))
	case Some(res) => { 
	  val r = isGoodWord(k.toInt, res)
	  val w = selectedLearningWords(k.toInt)

	  dao.updateLearningWord(userId, {if (r._1) Some(res.trim.toLowerCase) else None}, w, trans.get.get._1, trans.get.get._2)
	  
	  val score = dao.getLearningWordScore(userId, w, trans.get.get._1, trans.get.get._2)

	  mapB += (k -> (r._1 -> r._2 -> score))
	}
      }
    }

    if (map.isEmpty)
      println("map is empty")
    else
      println("map isn't empty")

    mapB.result
  }

  def wrongTranslation(sourceWord: Word, targetWord: Word, translation: EnabledTranslation): Boolean = { 
    val checkedSource = sourceWord.get match { 
      case None => null
      case Some(s) => s
    }

    val checkedTarget = targetWord.get match { 
      case None => null
      case Some(s) => s
    }

    val checkedTranslation = translation.get match { 
      case None => null
      case Some(s) => s
    }


    if (checkedTranslation == null || checkedTarget == null || checkedSource == null)
      false
    else
      dao.increaseFailTranslation(checkedSource, checkedTranslation._1, checkedTarget, checkedTranslation._2)
  }

  def listLearningWords(userId: Long) = { 
    dao.LearningWord(userId)
  }

  def removeLearningWord(userId: Long, name: String, srcLang: String, trgLang: String) { 
    dao.removeLearningWord(userId, name, srcLang, trgLang)
  }
}



import net.liftweb.http.SessionVar
import net.liftweb.common.{Box, Empty, Full}

import dao.entity.Word


trait SessionVarStorage { 
  object storage extends SessionVar[(HashMap[String, List[String]], 
				     List[String])](HashMap() -> List())

  def storeAllLearningWords(words: HashMap[String, List[String]]) { 
    storage(words -> storage.get._2)
  }

  def storeSelectedLearningWords(words: List[String]) { 
    storage(storage.get._1 -> words)
  }

  def unStoreAllLearningWords { 
    storage(HashMap() -> storage.get._2)
  }

  def unStoreSelectedLearningWords { 
    storage(storage.get._1 -> List())
  }

  def allLearningWords = storage.get._1

  def selectedLearningWords = storage.get._2

  def remove = storage.remove

}








