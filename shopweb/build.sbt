name := "shopweb"

organization := "idid"

version := "0.1"

scalaVersion := "2.11.2"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"

resolvers += "akka" at "http://repo.akka.io/snapshots"

libraryDependencies += "shift" % "shift-engine_2.11" % "0.1"

libraryDependencies += "shift" % "shift-html_2.11" % "0.1"

libraryDependencies += "shift" % "shift-netty_2.11" % "0.1"

libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2"
 
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3"

