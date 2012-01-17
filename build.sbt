name := "language-bis"
 
seq(webSettings: _*)

scanDirectories in Compile := Nil

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

resolvers += "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots"

resolvers += "Scala Tools Release" at "http://scala-tools.org/repo-releases"


libraryDependencies ++= {
  val liftVersion = "2.4-M5" // Put the current/latest lift version here
  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-widgets" % liftVersion % "compile->default"
    )
}

// Customize any further dependencies as desired
libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.3.v20111011" % "container, test", 
  // "com.h2database" % "h2" % "1.2.138",
  "ch.qos.logback" % "logback-classic" % "0.9.30" % "compile->default",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.squeryl" %% "squeryl" % "0.9.4",
  "postgresql" % "postgresql" % "8.4-701.jdbc4"
)

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

//scalacOptions += "-encoding"

//scalacOptions += "UTF-8"
