val sttpVersion = "1.1.14"

val circeVersion = "0.9.3"

val lintingSettings = Seq(
    "-unchecked",
    "-Xlint:_",
    "-Xfatal-warnings",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-extra-implicit",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
  name := "gemini-jobcoin",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion),
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp" %% "core",
    "com.softwaremill.sttp" %% "circe",
    "com.softwaremill.sttp" %% "async-http-client-backend-future"
  ).map(_ % sttpVersion),
  scalacOptions in (Compile, compile) := lintingSettings,
  scalacOptions in (Test, compile) := lintingSettings
)
