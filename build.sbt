ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.2"

// Set default class for "run"
Compile / mainClass := Some("gov.irs.twe.main")

scalafmtConfig := file(".scalafmt.conf")

// Also re-build on XML changes
// Doesn't work yet
// run / watchTriggers += baseDirectory.value.toGlob / "*.xml"

lazy val root = (project in file("."))
  .settings(
    name := "twe",

    // Core dependencies
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4",

    // Fact Graph!
    libraryDependencies += "gov.irs" %% "factgraph" % "3.1.0-SNAPSHOT",

    // Templating libraries
    libraryDependencies += "org.thymeleaf" % "thymeleaf" % "3.1.3.RELEASE",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.21.1",

    // JSON and YAML utilities
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
      ).map(_ % "0.14.15"),
    libraryDependencies += "io.circe" %% "circe-yaml" % "0.16.0",
    libraryDependencies += "io.circe" %% "circe-yaml-scalayaml" % "0.16.0",

    // CSV library for parsing scenario spreadsheets
    libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "2.0.0",

    // Local server
    libraryDependencies += "org.smol-utils" %% "smol" % "0.1.2",
    )
