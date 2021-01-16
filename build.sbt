import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    name := "net-wroth-calculator"
  )
  .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "net-wroth-calculator-test-suite",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      Libraries.scalaCheck,
      Libraries.scalaTest,
      Libraries.scalaTestPlus
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "net-wroth-calculator-core",
    packageName in Docker := "et-wroth-calculator",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsRetry,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.circeRefined,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
      Libraries.doobieCore,
      Libraries.elasticsearch,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
    )
  )
