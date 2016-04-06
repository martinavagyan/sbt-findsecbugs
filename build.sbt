sbtPlugin := true

name := "sbt-findsecbugs"
organization := "nl.codestar"
version := "0.1-SNAPSHOT"

scalaVersion := "2.10.6"
scalacOptions ++= Seq("-encoding", "UTF8", "-Xfatal-warnings",
  "-deprecation", "-feature", "-unchecked", "-Xlint",
  "-Ywarn-dead-code", "-Ywarn-adapted-args"
)

libraryDependencies += "com.google.code.findbugs" % "findbugs" % "3.0.1"
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.1"
libraryDependencies += "com.h3xstream.findsecbugs" % "findsecbugs-plugin" % "1.4.5"

// Scripted - sbt plugin tests
scriptedSettings
scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }