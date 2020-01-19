name := "eventer"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "dev.zio" %% "zio" % "1.0.0-RC17"
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.0-M6"
libraryDependencies += "org.http4s" %% "http4s-circe" % "0.21.0-M6"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.21.0-M6"
libraryDependencies += "com.github.pureconfig" % "pureconfig_2.13" % "0.12.2"
libraryDependencies += "io.circe" % "circe-core_2.13" % "0.13.0-M2"
libraryDependencies += "io.circe" % "circe-generic_2.13" % "0.13.0-M2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.0"
libraryDependencies += "io.getquill" %% "quill-jdbc" % "3.5.0"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.9"
libraryDependencies += "org.flywaydb" % "flyway-core" % "6.1.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test"

javaOptions += "-Duser.timezone=UTC"

fork := true
