name := """zipkin-play"""
organization := "starsy.info"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "net.jodah" % "failsafe" % "1.0.4"
libraryDependencies ++= Seq( javaWs )
libraryDependencies += "io.zipkin.brave" % "brave" % "4.1.1"
