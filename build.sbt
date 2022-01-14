import Dependencies._

lazy val root = (project in file("."))
  .settings(
    Compile / run / mainClass := Some("networthcalculator.Main")
  )
  .dependsOn(core)
  .aggregate(core)

lazy val tests = (project in file("modules/tests"))
  .configs(IntegrationTest)
  .settings(
    name := "net-worth-calculator-test-suite",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      Libraries.scalaTest
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "net-worth-calculator"
  )
  .settings(
    Docker / packageName := "net-worth-calculator",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts     := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
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

val commonSettings = Def.settings(
  inThisBuild(
    List(
      scalaVersion := "3.1.0",
      version      := "0.1.0-SNAPSHOT"
    )
  ),
  scalafmtOnCompile := true,
  addCommandAlias(
    "validate",
    List(
      "clean",
      "compile"
    ).mkString(";", "; ", "")
  ),
  addCommandAlias("run", "modules/core/run")
)
