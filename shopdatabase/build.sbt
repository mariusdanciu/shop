name := "shopdatabase"

organization := "idid"

version := "0.1"

scalaVersion := "2.11.2"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"

resolvers += "akka" at "http://repo.akka.io/snapshots"

libraryDependencies += "org.mongodb" % "casbah-core_2.11" % "2.7.3"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

libraryDependencies += "shift" % "shift-common_2.11" % "0.1"


