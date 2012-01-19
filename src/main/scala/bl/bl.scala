package bl

import lib.typesafe._

import scala.collection.immutable.HashMap
import scala.collection.mutable.Stack

  /** utilisation des parametres de type pour forcer tout les  couche visuel
   *  a les utilisé, permet aussi de deporter la verification dans
   *  la couche bl au lieux de vue
   */
abstract class BL {
  /** return all enabled translation (english-french but also french-english)
   */ 
  def allTranslations: List[(String, String)];

  def allEnableTranslations: List[(String, String)];

  def existTranslation(tr: (String, String)): Boolean;

  def allLanguages: List[(String, String)];

  def existLanguage(s: String): Boolean;

  def registerUser(name: UserName, password: Password, email: Email): HashMap[String, String];

  def addWord(word: Word, lang: Language): HashMap[String, String];

  def allWordsStartWith(lang: Language, w: Word): List[String];

  def addTranslation(sourceWord: Word, targetWord: ListWord, translation: EnabledTranslation): HashMap[String, String];

/*  def addLearningWord(userId: Long, words: Stack[Word], translation: EnabledTranslation): Option[String];
*/
  def login(userName: UserName, password: Password): Either[HashMap[String, String], Long];

/*  def quizz(userId: Long, n: Int, trans: EnabledTranslation): Either[HashMap[String, String], List[(String, List[String])]];

  def quizzAnswer(userId: Long, trans: EnabledTranslation, w1: Word, w2: Word, w3: Word, w4: Word, quizz: List[(String, List[String])]): Either[HashMap[String, String], List[Boolean]];
  */
  
}

import dao.entity.LearningWord

abstract class StorageLearningWord { 
  def storeAllLearningWords(words: List[LearningWord]);
  def storeSelectedLearningWords(words: List[LearningWord]);
  def unStoreAllLearningWords;
  def unStoreSelectedLearningWords;
  def remove;
}






