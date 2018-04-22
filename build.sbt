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
  scalaVersion := "2.11.2",
  resolvers ++= List(
    "mvnrepository" at "http://mvnrepository.com/artifact/",
    "akka" at "http://repo.akka.io/snapshots"
  )
)

lazy val root = (project in file("."))
  .aggregate(shopweb)

lazy val pack = TaskKey[Unit]("pack")


lazy val shopweb = (project in file("shopweb")).settings(
  commonSettings,
  name := "shopweb",
  libraryDependencies += "shift" % "shift-server_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "shift" % "shift-engine_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "shift" % "shift-html_2.11" % buildProps.getProperty("shift.version"),
  libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2",
  libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3",
  libraryDependencies += "io.netty" % "netty-all" % "4.1.17.Final",
  libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0",
  pack := {
    println("Packing ... ")

    IO.delete(distDir)
    IO.createDirectory(distDir)
    IO.createDirectory(libDir)

    val projectJar = packageBin in Compile value

    val cp = (managedClasspath in Runtime value).map {
      _.data
    } :+ projectJar

    for {jar <- cp} {
      println(s"Copying $jar")
      println("\tto: " + libDir / s"${jar.name}")
      IO.copyFile(jar, libDir / s"${jar.name}")
    }

    val showWebDir = new File("shopweb")

    IO.copyDirectory(showWebDir / "web", distDir / "web")
    IO.copyDirectory(showWebDir / "config", distDir / "config")
    IO.copyDirectory(showWebDir / "localization", distDir / "localization")

    IO.copyFile(scriptsDir / "start.sh", distDir / "start.sh")
    IO.copyFile(scriptsDir / "stop.sh", distDir / "stop.sh")

    val sv = scalaVersion value
    val v = version value

    makeTarGZ("target/idid_" + sv + "_" + v + "_.tar.gz", "idid_" + sv + "_" + v)
  }
)



val distDir = new File("./dist")
val libDir = distDir / "lib"
val scriptsDir = new File("./scripts")


import java.io._
import org.apache.commons.compress.archivers.tar._
import org.apache.commons.compress.compressors.gzip._


def makeTarGZ(name: String, folder: String) {
  val tOut = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(new File(name)))))
  try {
    populateTarGz(tOut, folder, "./dist")
  } finally {
    tOut.close()
  }
}


def populateTarGz(tOut: TarArchiveOutputStream, folder: String, path: String, base: String = null) {
  val f = new File(path);
  val entryName = if (base == null) folder else (base + f.getName())
  val tarEntry = new TarArchiveEntry(f, entryName)

  if (entryName.endsWith(".sh"))
    tarEntry.setMode(484)


  tOut.putArchiveEntry(tarEntry)

  if (f.isFile()) {
    IO.transfer(f, tOut)
    tOut.closeArchiveEntry()
  } else {
    tOut.closeArchiveEntry()
    val children = f.listFiles()
    if (children != null) {
      for (child <- children) {
        populateTarGz(tOut, folder, child.getAbsolutePath(), entryName + "/")
      }
    }
  }
}
