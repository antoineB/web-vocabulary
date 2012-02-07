package dao

import scala.collection.mutable.ListBuffer

import dao.entity._

abstract class DAO { 
  //----LANGUAGE----
  def allLanguages: List[Language];
  def existLanguage(name: String): Boolean;
  def addLanguage(lang: String, iso: String = ""): Boolean;
  def getLanguage(lang: String): Option[Language];
  //remove
  //update


  //----WORD----
  def allWords(langName: String, n: Option[Int] = None): List[Word];
  def allWordsStartWith(langName: String, start: String, n: Option[Int] = None): List[Word];
  def existWord(name: String, langName: String): Boolean;
  def getWord(name: String, langName: String): Option[Word];
  def addWord(name: String, langName: String): Boolean;
  //remove
  //update

  //----USER----
  def existUser(name: String): Boolean;
  def addUser(name: String, password: String, email: String): Boolean;
  def verifyUser(name: String, pass: String): Option[User];
  //remove
  //update



  //----LEARNING----
  def addLearningWord(wordName: String, userId: Long, sourceLanguage: String, targetLanguage: String): Boolean;

  /** Return the number of learning word a user have.
   */ 
  def numberLearningWords(userId: Long, trans: (String, String)): Long;

  /** Given an answer or not, update the learning word score.
   */ 
  def updateLearningWord(userId: Long, answer: Option[String], w: String, sourceLanguage: String, targetLanguage: String): Boolean;

  /** Return the score of a given learning word.
   */ 
  def getLearningWordScore(userId: Long, w: String, sourceLanguage: String, targetLanguage: String): (Int, Float);
  def archiveLearningWord(userId: Long, w: String, sourceLanguage: String, targetLanguage: String);

  def getLearning(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Option[LearningWord];

  //wrong name?
  def learningWord(userId: Long, sourceLanguage: String, targetLanguage: String): Iterable[(Word, List[Word])]; 

  def LearningWord(userId: Long): scala.collection.mutable.HashMap[String, ListBuffer[(String, List[String])]];
  def learningFail(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean;
  def learningSuccess(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean;
  def removeLearningWord(userId: Long, name: String, srcLang: String, trgLang: String): Boolean;




  //----TRANSLATION----

  /** Add a translation and the given words if they don't exist
   *  already.
   */ 
  def addTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean;

  /** Return all translation availables for a word from a language
   *  into another language.
   */ 
  def allWordTranslation(wordName: String, sourceLanguage: String, targetLanguage: String): List[Translation];

  /** Return all translation keeping the order they have.
   */ 
  def allEnableTranslation: List[(String, String)];
  def existTranslation(tr: (String, String)): Boolean;
  def allEnableTranslation(userId: Long): List[(String, String)];


  /** Test if a word in a given language have a translation into
   *  another word in a specified language.
   */ 
  def existTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean;

  def increaseFailTranslation(sourceWord: String, sourceLang: String, targetWord: String, targetLang: String): Boolean;

}
