import sbt._
import Keys._

lazy val buildProps = {
  import java.util.Properties

  val prop = new Properties()
  val file = new File("./build.properties")

  IO.load(prop, file)
  prop.setProperty("build", prop.getProperty("build").toInt + 1 + "");
  IO.write(prop, "", file)
  prop
}

lazy val commonSettings = Seq(
  organization := "com.idid",
  version := buildProps.getProperty("version") + "." + buildProps.getProperty("build"),
  scalaVersion := "2.12.4",
  resolvers ++= List(
    "mvnrepository" at "http://mvnrepository.com/artifact/",
    "akka" at "http://repo.akka.io/snapshots"
  )
)

lazy val root = (project in file("."))
  .aggregate(shopweb)

lazy val shopweb = (project in file("shopweb")).settings(
  commonSettings,
  name := "shopweb",
  libraryDependencies += "shift" % "shift-server_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "shift" % "shift-engine_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "shift" % "shift-html_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2",
  libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3",
  libraryDependencies += "io.netty" % "netty-all" % "4.1.17.Final",
  libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0"
)

val distShopWeb = TaskKey[File]("dist", "")

val distShopApiSetting = distShopWeb <<= (target, managedClasspath in Runtime, publishLocal, packageBin in Compile) map {
  (target, cp, _, pack) => {
    println(pack)
    println(cp)
    pack
  }
}