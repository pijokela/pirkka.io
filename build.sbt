name := """pirkka.io"""
organization := "io.pirkka"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.8.0"

libraryDependencies += "commons-codec" % "commons-codec" % "1.12"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.pirkka.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.pirkka.binders._"

EclipseKeys.withSource := true