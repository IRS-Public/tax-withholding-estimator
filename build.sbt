ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.6"

val twe = taskKey[Unit]("Build and output the TWE website")

lazy val root = (project in file("."))
  .settings(
    name := "form-flow",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    libraryDependencies += "gov.irs" %% "factgraph" % "3.1.0-SNAPSHOT",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.21.1",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4",

    // tasks
    twe := {

    }
  )
