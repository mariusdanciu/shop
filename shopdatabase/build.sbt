name := "shopdatabase"

organization := "idid"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"

resolvers += "akka" at "http://repo.akka.io/snapshots"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

libraryDependencies += "org.mongodb" %% "casbah" % "2.6.4"

libraryDependencies += "shift" % "shift-common_2.10" % "0.1"


