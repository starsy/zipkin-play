name := """zipkin-play"""
organization := "starsy.info"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "net.jodah" % "failsafe" % "1.0.4"
libraryDependencies ++= Seq( javaWs )
libraryDependencies += "io.zipkin.brave" % "brave" % "4.1.1"
libraryDependencies += "io.zipkin.brave" % "brave-okhttp" % "4.1.1"
libraryDependencies += "io.zipkin.brave" % "brave-spancollector-http" % "4.1.1"
libraryDependencies += "io.zipkin.reporter" % "zipkin-sender-okhttp3" % "0.6.13"
libraryDependencies += "junit" % "junit" % "4.12"
