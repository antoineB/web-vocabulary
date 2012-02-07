package dao

import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.MySQLAdapter
 
object Mysql {

  def startDbSession():Unit = {
    val dbUsername = "test"
    val dbPassword = "test"
    val dbConnection = "jdbc:mysql://localhost:3306/language?useUnicode=true&characterEncoding=utf8"

    Class.forName("com.mysql.jdbc.Driver")
    SessionFactory.concreteFactory = Some(
      () => 
	Session.create(
          java.sql.DriverManager.getConnection(dbConnection, dbUsername, dbPassword),
          new MySQLAdapter)
    )
  }
}
