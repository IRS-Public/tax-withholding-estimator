ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.6"

scalafmtConfig := file("project/.scalafmt.conf")

// Also re-build on XML changes
// Doesn't work yet
// run / watchTriggers += baseDirectory.value.toGlob / "*.xml"

lazy val root = (project in file("."))
  .settings(
    name := "twe",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    libraryDependencies += "gov.irs" %% "factgraph" % "3.1.0-SNAPSHOT",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.21.1",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4",
  )
