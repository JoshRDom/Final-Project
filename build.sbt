ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

val AkkaVersion = "2.6.20"

resolvers += ("custome1" at "http://4thline.org/m2").withAllowInsecureProtocol(true)

lazy val root = (project in file("."))
  .settings(
    name := "Final Project",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "org.fourthline.cling" % "cling-core" % "2.1.2",
      "org.fourthline.cling" % "cling-support" % "2.1.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "org.scalafx" %% "scalafxml-core-sfx8" % "0.5"
    )
  )

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

fork := true