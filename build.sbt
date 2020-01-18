name := "eventer"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "dev.zio" %% "zio" % "1.0.0-RC17"
libraryDependencies += "org.http4s" % "http4s-core_2.13" % "0.21.0-M6"
libraryDependencies += "com.github.pureconfig" % "pureconfig_2.13" % "0.12.2"
libraryDependencies += "io.circe" % "circe-core_2.13" % "0.13.0-M2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test"
