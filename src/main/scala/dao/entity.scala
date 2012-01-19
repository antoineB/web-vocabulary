package dao.entity

import org.squeryl.KeyedEntity

class Word(var name: String, var nbRelations: Int) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this("", 0)
  def this(s: String) = this(s, 0)
}

class TranslationTable(var name: String) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this("")
}

class Language(var name: String,
	       //iso 3166
	       var iso: String) extends KeyedEntity[Long] { 
  val id: Long = 0
  def this() = this("", "")
}
  
class Translation(val sourceWordId: Long,
		  val targetWordId: Long,
		  var nbRelations: Int) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this(0, 0, 0)
  def this(src: Long, trg: Long) = this(src, trg, 0)
}

class LearningWord(val userId: Long,
		   val wordId: Long,
		   val sourceLanguageId: Long,
		   val targetLanguageId: Long,
		   var fails: Int,
		   var success: Int,
		   var average: Float) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this(0, 0, 0, 0, 0, 0, 0.0F)
}

class TranslationScore(val learningWordId: Long,
		       val translationId: Long,
		       var success: Int) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this(0, 0, 0)
}

  
class User(var name: String,
	   var password: String,
	   var email: String
	 ) extends KeyedEntity[Long] {
  val id: Long = 0
  def this() = this("", "", "")
}


