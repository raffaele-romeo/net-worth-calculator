import Dependencies._
import sbtassembly.MergeStrategy

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    name := "net-worth-calculator"
  )
  .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "net-worth-calculator-test-suite",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    assembly / assemblyMergeStrategy := customMergeStrategy,
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
    name := "net-worth-calculator-core",
    Docker / packageName := "net-worth-calculator",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts := Seq(),
    dockerUpdateLatest := true,
    assembly / mainClass := Some("networthcalculator.Main"),
    assembly / assemblyMergeStrategy := customMergeStrategy,
    libraryDependencies ++= Seq(
      compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
      compilerPlugin(Libraries.betterMonadicFor),
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsRetry,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.cirisCore,
      Libraries.commonsCodec,
      Libraries.doobieCore,
      Libraries.doobieHikari,
      Libraries.doobiePostgres,
      Libraries.logback,
      Libraries.log4catsSlf4j,
      Libraries.log4catsCore,
      Libraries.nimbus,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
      Libraries.redis4catsEffects,
      Libraries.redis4catsLog4cats,
      Libraries.squants
    )
  )

def customMergeStrategy: String => MergeStrategy = {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", _) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
