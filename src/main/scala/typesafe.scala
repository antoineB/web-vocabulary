package lib.typesafe

import scala.util.matching.Regex
import java.security.MessageDigest

import bl.ConcreteBL


abstract class RegexChecker(valueIn: String) {
  protected var _value: String = beforeCheck(valueIn)

  protected def beforeCheck(s: String) = s.trim.toLowerCase
  
  protected def afterCheck(s: String) = s

  def regex: Regex;

  def get: Option[String] = {
    if (regex.findAllIn(_value).isEmpty)
      None
    else 
      Some(afterCheck(_value))
  }

  def value_=(s: String): Unit = { _value = beforeCheck(s) }
  def value = get
}

class UserName(value: String) extends RegexChecker(value) {
  override def beforeCheck(s: String) = s.trim
  def regex = """^[a-zA-Z0-9_-]+$""".r
}

object UserName { 
  val errMsg = "The user name isn't conform to the rule."
  val rule = "The user name can be composed with letter minor/upper case, digits, underscore and hyphen."
}

class Password(value: String) extends RegexChecker(value) {
  def regex = """^[a-zA-Z0-9_-]{4,}$""".r
  
  override def afterCheck(s: String) = MessageDigest.getInstance("SHA-1").digest(s.getBytes).foldLeft("")((str, i) => str + Integer.toHexString(0xFF & i))
}

object Password { 
  val errMsg = "The password isn't conform to the rule."
  val rule = "The password can be composed with letter minor/upper case, digits, underscore and hyphen. It must have a minimal lenght of 4."
}


class Email(value: String) extends RegexChecker(value) {
  def regex = """^([!#$%&'*+-/=?^_`{|}~A-Za-z0-9])+(\.([!#$%&'*+-/=?^_`{|}~A-Za-z0-9])+)*@([a-zA-Z0-9-])+(\.([a-zA-Z0-9-])+)*\.([a-zA-Z0-9]){2,}$""".r
}

object Email { 
  val errMsg = "The email isn't conform to the rule."
  val rule = "//TODO"
}

class Word(value: String) extends RegexChecker(value) { 
  def regex = """.{1,50}""".r
}

object Word { 
  val errMsg = "The word isn't conform to the rule."
  val rule = "A word can be composed with all character you want and have a lenght of 50 maximum."
}


class ListWord(valueIn: String) { 
   protected var _value: Array[String] = beforeCheck(valueIn)

  def regex = """.{1,50}""".r

  def beforeCheck(s: String) = s.split("\r\n").filter(_.trim != "").map(_.trim.toLowerCase)

  def afterCheck(a: Array[String]) = a.toList

  def get: Option[List[String]] = {
    if (_value.find(regex.findAllIn(_).isEmpty).isDefined)
      None
    else 
      Some(afterCheck(_value))
  }

  def value_=(s: String): Unit = { _value = beforeCheck(s) }
  def value = get

}

object ListWord { 
  val errMsg = "The word isn't conform to the rule."
  val rule = "A word can be composed with all character you want and have a lenght of 50 maximum."
}

class Language(valueIn: String) { 
  protected var _value: String = beforeCheck(valueIn)

  protected def beforeCheck(s: String) = s.trim.toLowerCase
  
  def get: Option[String] = {
    if (ConcreteBL.existLanguage(_value))
      Some(_value)
    else 
      None
  }

  def value_=(s: String): Unit = { _value = beforeCheck(s) }
  def value = get

}

object Language { 
  val errMsg = "//TODO"
  val rule = "//TODO"
}

class NewLanguage(value: String) extends RegexChecker(value) { 
  def regex = """[a-z]{1,25}""".r
}

object NewLanguage {
  val errMsg = "//TODO"
  val rule = "//TODO"
}

class NewIsoLanguage(value: String) extends RegexChecker(value) { 
  def regex = """[a-z]{1,3}""".r
}

object NewIsoLanguage { 
  val errMsg = "//TODO"
  val rule = "//TODO"
}


class EnabledTranslation(valueIn: String) { 
  protected var _value: String = beforeCheck(valueIn)

  protected def beforeCheck(s: String) = s
  
  protected def afterCheck(s: String) = ((s.split("-").head, s.split("-").last))

  def get: Option[(String, String)] = {
    val tmp = afterCheck(_value)
    if (ConcreteBL.existTranslation(tmp))
      Some(tmp)
    else 
      None
  }

  def value_=(s: String): Unit = { _value = beforeCheck(s) }
  def value = get

}

object EnabledTranslation { 
  val errMsg = "//TODO"
  val rule = "//TODO"
}
