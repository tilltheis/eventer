name := "eventer"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies += "dev.zio" %% "zio" % "1.0.3"
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "2.2.0.1"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.14"
libraryDependencies += "org.http4s" %% "http4s-circe" % "0.21.14"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.21.14"
libraryDependencies += "com.github.pureconfig" % "pureconfig_2.13" % "0.14.0"
libraryDependencies += "io.circe" % "circe-core_2.13" % "0.13.0"
libraryDependencies += "io.circe" % "circe-generic_2.13" % "0.13.0"
libraryDependencies += "io.circe" % "circe-parser_2.13" % "0.13.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.14.0"
libraryDependencies += "io.getquill" %% "quill-jdbc" % "3.5.3"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.18"
libraryDependencies += "org.flywaydb" % "flyway-core" % "7.3.2"
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
libraryDependencies += "com.pauldijou" %% "jwt-core" % "4.3.0"
libraryDependencies += "com.github.daddykotex" %% "courier" % "2.0.0"

libraryDependencies += "dev.zio" %% "zio-test" % "1.0.3" % Test
libraryDependencies += "dev.zio" %% "zio-test-sbt" % "1.0.1" % Test
libraryDependencies += "com.icegreen" % "greenmail" % "1.6.1" % Test

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

javaOptions += "-Duser.timezone=UTC"

fork := true

wartremoverErrors in (Compile, compile) := Warts.unsafe.diff(Seq(Wart.Any))
wartremoverErrors in (Test, compile) := Warts.unsafe.diff(Seq(Wart.Any, Wart.DefaultArguments))
