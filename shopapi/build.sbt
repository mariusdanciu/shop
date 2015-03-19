name := "shopapi"

organization := "idid"

version := buildProps.getProperty("version") + "." + buildProps.getProperty("build")

scalaVersion := "2.11.2"

libraryDependencies += "shift" % "shift-common_2.11" %  buildProps.getProperty("shift.version")

