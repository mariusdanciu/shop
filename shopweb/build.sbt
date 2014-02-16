name := "shopweb"

organization := "shop"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"

libraryDependencies += "shift" % "shift-engine_2.10"  % "0.1"

libraryDependencies += "shift" % "shift-html_2.10"  % "0.1"

libraryDependencies += "shift" % "shift-netty_2.10"  % "0.1"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.5"

libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2"



