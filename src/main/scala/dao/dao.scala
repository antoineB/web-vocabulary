package dao

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
//  def existUser(name: String): Option[User];
  def existUser(name: String): Boolean;
  def addUser(name: String, password: String, email: String): Boolean;
  def verifyUser(name: String, pass: String): Option[User];
  //remove
  //update

  //----LEARNING----
  def addLearningWord(wordName: String, userId: Long, sourceLanguage: String, targetLanguage: String): Boolean;


//  def allUserLearningWords(userId: Long); //what is it for?

  def allUserLearningWord(userId: Long); //what is it for?

  def learningWord(userId: Long, limit: Int, sourceLanguage: String, targetLanguage: String): Iterable[(Word, List[Word])]; //wrong name?
  def learningFail(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean;
  def learningSuccess(userId: Long, name: String, sourceLanguage: String, targetLanguage: String): Boolean;
  //remove
  //there will never be update enable

  //----TRANSLATION----

  //add a translation and words if they don't exist
  def addTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean;
//  def allWordTranslations(wordName: String, sourceLanguage: String, targetLanguage: String): List[Translation];
  def allWordTranslation(wordName: String, sourceLanguage: String, targetLanguage: String): List[Translation];

  //need explanation
  def allEnableTranslation: List[(String, String)];
  def existTranslation(tr: (String, String)): Boolean;
  def allEnableTranslation(userId: Long): List[(String, String)];

  def numberLearningWords(userId: Long, trans: (String, String)): Long;

  def updateLearningWord(userId: Long, answer: Option[String], w: String, sourceLanguage: String, targetLanguage: String): Boolean;

  def existTranslation(sourceWordName: String, sourceLanguage: String, targetWordName: String, targetLanguage: String): Boolean;

  def increaseFailTranslation(sourceWord: String, sourceLang: String, targetWord: String, targetLang: String): Boolean;

  def getLearningWordScore(userId: Long, w: String, sourceLanguage: String, targetLanguage: String): Float;
}
