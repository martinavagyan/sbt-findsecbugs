sbtPlugin := true

name := "sbt-findsecbugs"
organization := "nl.codestar"
version := "0.1"

scalaVersion := "2.11.7"
scalacOptions ++= Seq("-encoding", "UTF8", "-Xfatal-warnings",
  "-deprecation", "-feature", "-unchecked", "-Xlint",
  "-Ywarn-dead-code", "-Ywarn-unused", "-Ywarn-adapted-args"
)
